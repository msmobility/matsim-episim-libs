package org.matsim.episim;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.episim.data.*;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.time.DayOfWeek;
import java.util.*;

/**
 * This class uses a MATSim scenario to create events relevant for episim and infection dynamics.
 */
public class EventsFromMATSimScenario implements EpisimEventProvider {

	private static final Logger log = LogManager.getLogger(EventsFromMATSimScenario.class);

	/**
	 * MATSim input events.
	 */
	private final Map<DayOfWeek, List<Event>> events;

	/**
	 * See {@link #getPersonIds()} ()}
	 */
	private final Map<Id<Person>, MutableEpisimPerson> personMap = new IdMap<>(Person.class);
	private final Map<Id<Vehicle>, MutableEpisimContainer> vehicleMap = new IdMap<>(Vehicle.class);
	private final Map<Id<ActivityFacility>, MutableEpisimContainer> pseudoFacilityMap = new IdMap<>(ActivityFacility.class,
			// the number of facility ids is not known beforehand, so we use this as initial estimate
			(int) (Id.getNumberOfIds(Vehicle.class) * 1.3));
	private Map<Id<EpisimContainer>, MutableEpisimContainer> containerMap;
	/**
	 * Maps activity type to its parameter.
	 * This can be an identity map because the strings are canonicalized by the {@link InputEventProvider}.
	 */
	private final Map<String, MutableEpisimPerson.Activity> paramsMap = new IdentityHashMap<>();

	/**
	 * Whether init was performed.
	 */
	private boolean init = false;

	private final EpisimConfigGroup episimConfig;
	private final Scenario scenario;
	private final EpisimReporting reporting;


	/**
	 * Whether {@code event} should be handled.
	 *
	 * @param actType activity type
	 */
	public static boolean shouldHandleActivityEvent(HasPersonId event, String actType) {
		// ignore drt and stage activities
		return !event.getPersonId().toString().startsWith("drt") && !event.getPersonId().toString().startsWith("rt")
				&& !TripStructureUtils.isStageActivityType(actType);
	}

	/**
	 * Whether a Person event (e.g. {@link PersonEntersVehicleEvent} should be handled.
	 */
	public static boolean shouldHandlePersonEvent(HasPersonId event) {
		// ignore pt drivers and drt
		String id = event.getPersonId().toString();
		return !id.startsWith("pt_pt") && !id.startsWith("pt_tr") && !id.startsWith("drt") && !id.startsWith("rt");
	}

	@Inject
	public EventsFromMATSimScenario(InputEventProvider provider, Scenario scenario, Config config, EpisimReporting reporting) {
		this.events = provider.getEvents();
		this.episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		this.scenario = scenario;
		this.reporting = reporting;
	}

	@Override
	public void init() {

		Object2IntMap<MutableEpisimContainer> groupSize = new Object2IntOpenHashMap<>();
		Object2IntMap<MutableEpisimContainer> totalUsers = new Object2IntOpenHashMap<>();
		Object2IntMap<MutableEpisimContainer> maxGroupSize = new Object2IntOpenHashMap<>();

		Map<EpisimContainer, Object2IntMap<String>> activityUsage = new HashMap<>();

		Map<List<Event>, DayOfWeek> sameDay = new IdentityHashMap<>(7);

		for (Map.Entry<DayOfWeek, List<Event>> entry : events.entrySet()) {

			DayOfWeek day = entry.getKey();
			List<Event> eventsForDay = entry.getValue();

			if (sameDay.containsKey(eventsForDay)) {
				DayOfWeek same = sameDay.get(eventsForDay);
				log.info("Init Day {} same as {}", day, same);
				this.personMap.values().forEach(p -> p.duplicateDay(day, same));
				continue;
			}

			log.info("Init day {}", day);

			this.personMap.values().forEach(p -> p.setStartOfDay(day, p.getCurrentPositionInTrajectory()));

			for (Event event : eventsForDay) {

				MutableEpisimPerson person = null;
				MutableEpisimContainer facility = null;

				// Add all person and facilities
				if (event instanceof HasPersonId) {
					if (!shouldHandlePersonEvent((HasPersonId) event)) continue;

					person = this.personMap.computeIfAbsent(((HasPersonId) event).getPersonId(), MutableEpisimPerson::new);

					// If a person was added late, previous days are initialized at home
					for (int i = 1; i < day.getValue(); i++) {
						DayOfWeek it = DayOfWeek.of(i);
						if (person.getFirstFacilityId(it) == null) {
							person.setStartOfDay(it, person.getCurrentPositionInTrajectory());
							person.setEndOfDay(it, person.getCurrentPositionInTrajectory());
							person.setFirstFacilityId(createHomeFacility(person).getContainerId(), it);
							EpisimPerson.Activity home = paramsMap.computeIfAbsent("home", this::createActivityType);
							person.addToTrajectory(home);
						}
					}
				}

				if (event instanceof HasFacilityId) {
					Id<ActivityFacility> episimFacilityId = createEpisimFacilityId((HasFacilityId) event);
					facility = this.pseudoFacilityMap.computeIfAbsent(episimFacilityId, MutableEpisimContainer::new);
				}

				if (event instanceof ActivityStartEvent) {

					String actType = ((ActivityStartEvent) event).getActType();
					if (!shouldHandleActivityEvent((HasPersonId) event, actType))
						continue;

					EpisimPerson.Activity act = paramsMap.computeIfAbsent(actType, this::createActivityType);
					totalUsers.mergeInt(facility, 1, Integer::sum);

					handleEvent((ActivityStartEvent) event);

				} else if (event instanceof ActivityEndEvent) {
					String actType = ((ActivityEndEvent) event).getActType();
					if (!shouldHandleActivityEvent((HasPersonId) event, actType))
						continue;

					EpisimPerson.Activity act = paramsMap.computeIfAbsent(actType, this::createActivityType);
					activityUsage.computeIfAbsent(facility, k -> new Object2IntOpenHashMap<>()).mergeInt(actType, 1, Integer::sum);

					// Add person to container if it starts its day with end activity
					if (person.getFirstFacilityId(day) == null) {
						// person may already be there because of previous day
						if (person.getCurrentContainer() != facility) {

							// remove from old
							if (person.getCurrentContainer() != null)
								person.getCurrentContainer().removePerson(person);

							facility.addPerson(person, 0);
						}

						person.setFirstFacilityId(facility.getContainerId(), day);
					}

					handleEvent((ActivityEndEvent) event);
				}

				if (event instanceof PersonEntersVehicleEvent) {
					if (!shouldHandlePersonEvent((HasPersonId) event)) continue;

					MutableEpisimContainer vehicle = this.vehicleMap.computeIfAbsent(((PersonEntersVehicleEvent) event).getVehicleId(),
							d -> new MutableEpisimContainer(d, true));

					maxGroupSize.mergeInt(vehicle, groupSize.mergeInt(vehicle, 1, Integer::sum), Integer::max);
					totalUsers.mergeInt(vehicle, 1, Integer::sum);

					handleEvent((PersonEntersVehicleEvent) event);

				} else if (event instanceof PersonLeavesVehicleEvent) {
					if (!shouldHandlePersonEvent((HasPersonId) event)) continue;

					MutableEpisimContainer vehicle = this.vehicleMap.computeIfAbsent(((PersonLeavesVehicleEvent) event).getVehicleId(),
							d -> new MutableEpisimContainer(d, true));
					groupSize.mergeInt(vehicle, -1, Integer::sum);
					activityUsage.computeIfAbsent(vehicle, k -> new Object2IntOpenHashMap<>()).mergeInt("tr", 1, Integer::sum);

					handleEvent((PersonLeavesVehicleEvent) event);
				}
			}

			int cnt = 0;
			for (MutableEpisimPerson person : this.personMap.values()) {
				List<EpisimPerson.Activity> tj = person.getTrajectory();

				// person that didn't move will be put at home the whole day
				if (person.getFirstFacilityId(day) == null && person.getCurrentPositionInTrajectory() == person.getStartOfDay(day)) {
					EpisimPerson.Activity home = paramsMap.computeIfAbsent("home", this::createActivityType);
					person.addToTrajectory(home);
					person.incrementCurrentPositionInTrajectory();
					MutableEpisimContainer facility = createHomeFacility(person);
					person.setFirstFacilityId(facility.getContainerId(), day);
					cnt++;
				}

				// close open trajectories by repeating last element
				if (tj.size() == person.getCurrentPositionInTrajectory()) {
					person.addToTrajectory(tj.get(tj.size() - 1));
					person.incrementCurrentPositionInTrajectory();

					if (person.getFirstFacilityId(day) == null)
						person.setFirstFacilityId(createHomeFacility(person).getContainerId(), day);
				}

				person.setEndOfDay(day, tj.size() - 1);
			}

			log.info("Persons stationary on {}: {} ({}%)", day, cnt, cnt * 100.0 / personMap.size());

			sameDay.put(eventsForDay, day);
		}

		insertStationaryAgents();

		// Add missing facilities, with only stationary agents
		for (MutableEpisimContainer facility : pseudoFacilityMap.values()) {
			if (!maxGroupSize.containsKey(facility)) {
				totalUsers.put(facility, facility.getPersons().size());
				maxGroupSize.put(facility, facility.getPersons().size());

				// there may be facilities with only "end" events, thus no group size, but correct activity usage
				if (!activityUsage.containsKey(facility)) {
					Object2IntOpenHashMap<String> act = new Object2IntOpenHashMap<>();
					act.put("home", facility.getPersons().size());
					activityUsage.put(facility, act);
				}
			}
		}

		containerMap = new IdMap<>(EpisimContainer.class, pseudoFacilityMap.size() + vehicleMap.size());

		pseudoFacilityMap.forEach((k, v) -> containerMap.put(v.getContainerId(), v));
		vehicleMap.forEach((k, v) -> containerMap.put(v.getContainerId(), v));

		// Go through each day again to compute max group sizes
		sameDay.clear();
		for (Map.Entry<DayOfWeek, List<Event>> entry : events.entrySet()) {

			DayOfWeek day = entry.getKey();
			List<Event> eventsForDay = entry.getValue();

			if (sameDay.containsKey(eventsForDay)) {
				continue;
			}

			personMap.values().forEach(p -> {
				checkAndHandleEndOfNonCircularTrajectory(p, day);
				p.resetCurrentPositionInTrajectory(day);
			});

			pseudoFacilityMap.forEach((k, v) -> maxGroupSize.mergeInt(v, v.getPersons().size(), Integer::max));

			for (Event event : eventsForDay) {
				if (event instanceof HasFacilityId && event instanceof HasPersonId) {
					MutableEpisimContainer facility = pseudoFacilityMap.get(((HasFacilityId) event).getFacilityId());

					// happens on filtered events that are not relevant
					if (facility == null)
						continue;

					if (event instanceof ActivityStartEvent) {
						handleEvent((ActivityStartEvent) event);
						maxGroupSize.mergeInt(facility, facility.getPersons().size(), Integer::max);
					} else if (event instanceof ActivityEndEvent) {
						handleEvent((ActivityEndEvent) event);
					}
				}
			}

			sameDay.put(eventsForDay, day);
		}

		log.info("Computed max group sizes");

		reporting.reportContainerUsage(maxGroupSize, totalUsers, activityUsage);

		boolean useVehicles = !scenario.getVehicles().getVehicles().isEmpty();

		log.info("Using capacity from vehicles file: {}", useVehicles);

		// these always needs to be present
		paramsMap.computeIfAbsent("tr", this::createActivityType);
		paramsMap.computeIfAbsent("home", this::createActivityType);

		// entry for undefined activity type
		AbstractObject2IntMap.BasicEntry<String> undefined = new AbstractObject2IntMap.BasicEntry<>("undefined", -1);

		for (Object2IntMap.Entry<MutableEpisimContainer> kv : maxGroupSize.object2IntEntrySet()) {

			MutableEpisimContainer container = kv.getKey();
			double scale = 1 / episimConfig.getSampleSize();

			container.setTotalUsers((int) (totalUsers.getInt(container) * scale));
			container.setMaxGroupSize((int) (kv.getIntValue() * scale));

			Object2IntMap<String> usage = activityUsage.get(kv.getKey());
			if (usage != null) {
				Object2IntMap.Entry<String> max = usage.object2IntEntrySet().stream()
						.reduce(undefined, (s1, s2) -> s1.getIntValue() > s2.getIntValue() ? s1 : s2);

				if (max != undefined) {
					// set container spaces to spaces of most used activity
					EpisimPerson.Activity act = paramsMap.get(max.getKey());
					if (act == null)
						log.warn("No activity found for {}", max.getKey());
					else
						container.setNumSpaces(act.params.getSpacesPerFacility());
				}
			}

			if (useVehicles && container.isVehicle()) {

				Id<Vehicle> vehicleId = Id.createVehicleId(container.getContainerId().toString());
				Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehicleId);

				if (vehicle == null) {
					log.warn("No type found for vehicleId={}; using capacity of 150.", vehicleId);
					container.setTypicalCapacity(150);
				} else {
					int capacity = vehicle.getType().getCapacity().getStandingRoom() + vehicle.getType().getCapacity().getSeats();
					container.setTypicalCapacity(capacity);
				}
			}
		}

		// Clear time-use after first iteration
		personMap.values().forEach(p -> p.getSpentTime().clear());

		init = true;
	}

	/**
	 * Creates the home facility of a person.
	 */
	private MutableEpisimContainer createHomeFacility(MutableEpisimPerson person) {
		String homeId = (String) person.getAttributes().getAttribute("homeId");
		if (homeId == null)
			homeId = "home_of_" + person.getPersonId().toString();

		Id<ActivityFacility> facilityId = Id.create(homeId, ActivityFacility.class);
		// add facility that might not exist yet
		return this.pseudoFacilityMap.computeIfAbsent(facilityId, MutableEpisimContainer::new);
	}

	private MutableEpisimPerson.Activity createActivityType(String actType) {
		return new MutableEpisimPerson.Activity(actType, episimConfig.selectInfectionParams(actType));
	}

	private Id<ActivityFacility> createEpisimFacilityId(HasFacilityId event) {
		if (episimConfig.getFacilitiesHandling() == EpisimConfigGroup.FacilitiesHandling.snz) {
			Id<ActivityFacility> id = event.getFacilityId();
			if (id == null)
				throw new IllegalStateException("No facility id present. Please switch to episimConfig.setFacilitiesHandling( EpisimConfigGroup.FacilitiesHandling.bln ) ");

			return id;
		} else if (episimConfig.getFacilitiesHandling() == EpisimConfigGroup.FacilitiesHandling.bln) {
			// TODO: this has poor performance and should be preprocessing...
			if (event instanceof ActivityStartEvent) {
				ActivityStartEvent theEvent = (ActivityStartEvent) event;
				return Id.create(theEvent.getActType().split("_")[0] + "_" + theEvent.getLinkId().toString(), ActivityFacility.class);
			} else if (event instanceof ActivityEndEvent) {
				ActivityEndEvent theEvent = (ActivityEndEvent) event;
				return Id.create(theEvent.getActType().split("_")[0] + "_" + theEvent.getLinkId().toString(), ActivityFacility.class);
			} else {
				throw new IllegalStateException("unexpected event type=" + ((Event) event).getEventType());
			}
		} else {
			throw new NotImplementedException(Gbl.NOT_IMPLEMENTED);
		}

	}

	private void handlePersonTrajectory(Id<Person> personId, EpisimPerson.Activity trajectoryElement) {
		MutableEpisimPerson person = personMap.get(personId);

		if (person.getCurrentPositionInTrajectory() + 1 == person.getTrajectory().size()) {
			return;
		}
		person.incrementCurrentPositionInTrajectory();
		if (init) {
			return;
		}

		person.addToTrajectory(trajectoryElement);
	}

	/**
	 * Insert agents that appear in the population, but not in the event file, into their home container.
	 */
	private void insertStationaryAgents() {

		int inserted = 0;
		int skipped = 0;
		for (Person p : scenario.getPopulation().getPersons().values()) {

			if (!personMap.containsKey(p.getId())) {
				String homeId = (String) p.getAttributes().getAttribute("homeId");

				if (homeId != null) {

					Id<ActivityFacility> facilityId = Id.create(homeId, ActivityFacility.class);
					MutableEpisimContainer facility = pseudoFacilityMap.computeIfAbsent(facilityId, MutableEpisimContainer::new);
					MutableEpisimPerson episimPerson = personMap.computeIfAbsent(p.getId(), MutableEpisimPerson::new);

					// Person stays here the whole week
					for (DayOfWeek day : DayOfWeek.values()) {
						episimPerson.setFirstFacilityId(facility.getContainerId(), day);
					}

					episimPerson.addToTrajectory(new MutableEpisimPerson.Activity("home", paramsMap.get("home").params));

					facility.addPerson(episimPerson, 0);

					inserted++;
				} else
					skipped++;
			}
		}

		if (skipped > 0)
			log.warn("Ignored {} stationary agents, because of missing home ids", skipped);

		log.info("Inserted {} stationary agents, total = {}", inserted, personMap.size());
	}

	private boolean handleEvent(ActivityStartEvent activityStartEvent) {
//		double now = activityStartEvent.getTime();
		double now = EpisimUtils.getCorrectedTime(episimConfig.getStartOffset(), activityStartEvent.getTime(), 0);

		if (!shouldHandleActivityEvent(activityStartEvent, activityStartEvent.getActType())) {
			return false;
		}

		// find the person:
		MutableEpisimPerson episimPerson = this.personMap.get(activityStartEvent.getPersonId());

		// create pseudo facility id that includes the activity type:
		Id<ActivityFacility> episimFacilityId = createEpisimFacilityId(activityStartEvent);

		// find the facility
		MutableEpisimContainer episimFacility = this.pseudoFacilityMap.get(episimFacilityId);

		// add person to facility
		episimFacility.addPerson(episimPerson, now);

		handlePersonTrajectory(episimPerson.getPersonId(), paramsMap.get(activityStartEvent.getActType()));

		return true;
	}


	private boolean handleEvent(ActivityEndEvent activityEndEvent) {
//		double now = activityEndEvent.getTime();
		double now = EpisimUtils.getCorrectedTime(episimConfig.getStartOffset(), activityEndEvent.getTime(), 0);

		if (!shouldHandleActivityEvent(activityEndEvent, activityEndEvent.getActType())) {
			return false;
		}

		MutableEpisimPerson episimPerson = this.personMap.get(activityEndEvent.getPersonId());
		Id<ActivityFacility> episimFacilityId = createEpisimFacilityId(activityEndEvent);

		MutableEpisimContainer episimFacility = episimPerson.getCurrentContainer();
		if (!episimFacility.equals(pseudoFacilityMap.get(episimFacilityId))) {
			throw new IllegalStateException("Person=" + episimPerson.getPersonId().toString() + " has activity end event at facility=" + episimFacilityId + " but actually is at facility=" + episimFacility.getContainerId().toString());
		}

		if (!init) {
			double timeSpent = now - episimFacility.getContainerEnteringTime(episimPerson.getPersonId());
			episimPerson.addSpentTime(activityEndEvent.getActType(), timeSpent);
			episimFacility.removePerson(episimPerson);
			handlePersonTrajectory(episimPerson.getPersonId(), paramsMap.get(activityEndEvent.getActType()));
		}

		return true;
	}

	private boolean handleEvent(PersonEntersVehicleEvent entersVehicleEvent) {
//		double now = entersVehicleEvent.getTime();
		double now = EpisimUtils.getCorrectedTime(episimConfig.getStartOffset(), entersVehicleEvent.getTime(), 0);

		if (!shouldHandlePersonEvent(entersVehicleEvent)) {
			return false;
		}

		// find the person:
		MutableEpisimPerson episimPerson = this.personMap.get(entersVehicleEvent.getPersonId());

		// find the vehicle:
		MutableEpisimContainer episimVehicle = this.vehicleMap.get(entersVehicleEvent.getVehicleId());

		// add person to vehicle and memorize entering time:
		episimVehicle.addPerson(episimPerson, now);

		return true;
	}


	private boolean handleEvent(PersonLeavesVehicleEvent leavesVehicleEvent) {
//		double now = leavesVehicleEvent.getTime();
		double now = EpisimUtils.getCorrectedTime(episimConfig.getStartOffset(), leavesVehicleEvent.getTime(), 0);

		if (!shouldHandlePersonEvent(leavesVehicleEvent)) {
			return false;
		}

		// find vehicle:
		MutableEpisimContainer episimVehicle = this.vehicleMap.get(leavesVehicleEvent.getVehicleId());

		MutableEpisimPerson episimPerson = this.personMap.get(leavesVehicleEvent.getPersonId());

		double timeSpent = now - episimVehicle.getContainerEnteringTime(episimPerson.getPersonId());

		if (!init) {
			// This type depends on the params defined in the scenario
			episimPerson.addSpentTime("pt", timeSpent);
			// remove person from vehicle:
			episimVehicle.removePerson(episimPerson);
		}

		return true;
	}

	/**
	 * Handle plans with "holes" in their trajectory.
	 *
	 * @param day day that is about to start
	 */
	private void checkAndHandleEndOfNonCircularTrajectory(MutableEpisimPerson person, DayOfWeek day) {
		Id<EpisimContainer> firstFacilityId = person.getFirstFacilityId(day);

		// TODO: map of container ids
		// TODO #######################
		int iteration = 0;

		// now is the start of current day, when this is called iteration still has the value of the last day
		double now = EpisimUtils.getCorrectedTime(episimConfig.getStartOffset(), 0, iteration + 1);

		if (person.isInContainer()) {
			MutableEpisimContainer container = person.getCurrentContainer();
			Id<EpisimContainer> lastFacilityId = container.getContainerId();

			if (container.isFacility() && this.containerMap.containsKey(lastFacilityId) && !firstFacilityId.equals(lastFacilityId)) {
				MutableEpisimContainer lastFacility = this.containerMap.get(lastFacilityId);

				// index of last activity at previous day
				int index = person.getEndOfDay(day.minus(1));
				String actType = person.getTrajectory().get(index).actType;

				double timeSpent = now - lastFacility.getContainerEnteringTime(person.getPersonId());
				person.addSpentTime(actType, timeSpent);

				if (iteration > 1 && timeSpent > 86400 && !actType.equals("home")) {
					// there might be some implausible trajectories
					log.trace("{} spent {} outside home", person, timeSpent);
				}

				lastFacility.removePerson(person);
				MutableEpisimContainer firstFacility = this.containerMap.get(firstFacilityId);
				firstFacility.addPerson(person, now);

			} else if (container.isVehicle() && this.containerMap.containsKey(lastFacilityId)) {
				MutableEpisimContainer lastVehicle = this.containerMap.get(lastFacilityId);
				person.addSpentTime("pt", now - lastVehicle.getContainerEnteringTime(person.getPersonId()));

				lastVehicle.removePerson(person);
				MutableEpisimContainer firstFacility = this.containerMap.get(firstFacilityId);
				firstFacility.addPerson(person, now);
			}
		} else {
			MutableEpisimContainer firstFacility = this.containerMap.get(firstFacilityId);
			firstFacility.addPerson(person, now);
		}
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public Set<Id<Person>> getPersonIds() {
		return personMap.keySet();
	}

	@Override
	public Set<EpisimContainer> getContainer() {
		return Sets.newHashSet(containerMap.values());
	}

	@Override
	public Iterable<PersonLeavesContainerEvent> forDay(DayOfWeek day) {

		// TODO: needs to be in iterator and generate events
		// TODO ###################
		for (MutableEpisimPerson person : personMap.values()) {
			checkAndHandleEndOfNonCircularTrajectory(person, day);
			person.resetCurrentPositionInTrajectory(day);
		}

		List<Event> events = this.events.get(day);
		return () -> new EventsIterator(events.iterator());
	}

	/**
	 * Generates events based on matsim input events.
	 */
	private class EventsIterator implements Iterator<PersonLeavesContainerEvent> {

		private final Iterator<Event> it;
		private final PersonLeaves event = new PersonLeaves();


		public EventsIterator(Iterator<Event> events) {
			this.it = events;
		}

		@Override
		public boolean hasNext() {

			// remove person from previous event
			if (event.container != null) {
				event.container.removePerson(event.person);

				if (event.container.isFacility())
					handlePersonTrajectory(event.person.getPersonId(),
							event.person.getTrajectory().get(event.person.getCurrentPositionInTrajectory()));


				event.reset();
			}

			while (it.hasNext()) {

				Event next = it.next();

				// no action necessary
				if (next instanceof ActivityStartEvent) {
					handleEvent((ActivityStartEvent) next);
				} else if (next instanceof PersonEntersVehicleEvent)
					handleEvent((PersonEntersVehicleEvent) next);
				else if (next instanceof ActivityEndEvent) {

					ActivityEndEvent e = (ActivityEndEvent) next;

					if (!shouldHandleActivityEvent(e, e.getActType()))
						continue;

					Id<ActivityFacility> episimFacilityId = createEpisimFacilityId((HasFacilityId) next);
					MutableEpisimContainer episimContainer = pseudoFacilityMap.get(episimFacilityId);
					MutableEpisimPerson episimPerson = personMap.get(e.getPersonId());

					event.setContext((int) e.getTime(), episimPerson, episimContainer);
					return true;
				} else if (next instanceof PersonLeavesVehicleEvent) {

					PersonLeavesVehicleEvent e = (PersonLeavesVehicleEvent) next;

					if (!shouldHandlePersonEvent(e))
						continue;

					MutableEpisimContainer episimContainer = vehicleMap.get(e.getVehicleId());
					MutableEpisimPerson episimPerson = personMap.get(e.getPersonId());

					event.setContext((int) e.getTime(), episimPerson, episimContainer);
					return true;
				}
			}

			return false;
		}

		@Override
		public PersonLeavesContainerEvent next() {
			return event;
		}
	}

	private static class PersonLeaves implements PersonLeavesContainerEvent {

		private int now;
		private MutableEpisimPerson person;
		private MutableEpisimContainer container;

		private PersonLeaves setContext(int now, MutableEpisimPerson person, MutableEpisimContainer container) {
			this.now = now;
			this.person = person;
			this.container = container;
			return this;
		}

		private void reset() {
			this.now = -1;
			this.person = null;
			this.container = null;
		}

		@Override
		public EpisimContainer getContainer() {
			return container;
		}

		@Override
		public EpisimConfigGroup.InfectionParams getActivity() {
			return person.getTrajectory().get(person.getCurrentPositionInTrajectory()).params;
		}

		@Override
		public int getLeaveTime() {
			return now;
		}

		@Override
		public int getEnterTime() {
			return (int) container.getContainerEnteringTime(person.getPersonId());
		}

		@Override
		public int getNumberOfContacts() {
			return container.getPersons().size() - 1;
		}

		@Override
		public Iterator<PersonContact> iterator() {
			// TODO
			// TODO ##############################
			return null;
		}

		@Override
		public Id<Person> getPersonId() {
			return person.getPersonId();
		}
	}


}
