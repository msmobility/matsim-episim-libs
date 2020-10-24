/*-
 * #%L
 * MATSim Episim
 * %%
 * Copyright (C) 2020 matsim-org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.matsim.episim.model;

import com.google.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.episim.*;

import java.util.*;

import org.matsim.episim.data.DiseaseStatus;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.PersonEntersContainerEvent;
import org.matsim.episim.data.PersonLeavesContainerEvent;

/**
 * Model where persons are only interacting pairwise.
 */
public final class DirectContactModel extends AbstractContactModel {

	private static final Logger log = LogManager.getLogger(DirectContactModel.class);

	/**
	 * Flag to enable tracking, which is considerably slower.
	 */
	private final int trackingAfterDay;

	/**
	 * Whether to trace susceptible persons.
	 */
	private final boolean traceSusceptible;

	/**
	 * This buffer is used to store the infection type.
	 */
	private final StringBuilder buffer = new StringBuilder();

	private final Map<EpisimContainer, MutableEpisimPerson> singlePersons = new IdentityHashMap<>();
	private final Map<EpisimContainer, List<Group>> groups = new IdentityHashMap<>();

	@Inject
		/*package*/ DirectContactModel(SplittableRandom rnd, Config config, TracingConfigGroup tracingConfig,
									   EpisimReporting reporting, InfectionModel infectionModel) {
		super(rnd, config, infectionModel, reporting);
		this.trackingAfterDay = tracingConfig.getPutTraceablePersonsInQuarantineAfterDay();
		this.traceSusceptible = tracingConfig.getTraceSusceptible();
	}


	@Override
	public void notifyEnterContainer(PersonEntersContainerEvent event, double now) {

		if (event.getContainer().isFacility())
			notifyEnterContainerGeneralized(persons.get(event.getPersonId()), event.getContainer(), now);
	}

	private void notifyEnterContainerGeneralized(MutableEpisimPerson personEnteringContainer, EpisimContainer container, double now) {

		// this can happen because persons are not removed during initialization
		if (findGroup(container, personEnteringContainer) != null)
			return;

		// for same reason a person currently at home will enter again
		if (!singlePersons.containsKey(container) || singlePersons.get(container) == personEnteringContainer) {
			singlePersons.put(container, personEnteringContainer);
		} else {
			groups.computeIfAbsent(container, k -> new ArrayList<>())
					.add(Group.of(personEnteringContainer, singlePersons.get(container), now));
			singlePersons.remove(container);
		}
	}

	private Group findGroup(EpisimContainer container, MutableEpisimPerson person) {

		if (!groups.containsKey(container))
			return null;

		for (Group group : groups.get(container)) {
			if (group.contains(person)) {
				return group;
			}
		}

		return null;
	}

	@Override
	public void infectionDynamicsContainer(PersonLeavesContainerEvent event, double now) {

		// no infection possible if there is only one person
		if (iteration == 0 || event.getNumberOfContacts() == 0) {
			removePersonFromGroups(event.getContainer(), persons.get(event.getPersonId()), now);
			return;
		}

		MutableEpisimPerson personLeavingContainer = persons.get(event.getPersonId());
		EpisimContainer container = event.getContainer();

		if (singlePersons.get(container) == personLeavingContainer) {
			singlePersons.remove(container);
			return;
		}

		if (!personRelevantForTrackingOrInfectionDynamics(personLeavingContainer, event, getRestrictions(), rnd)) {
			removePersonFromGroups(container, personLeavingContainer, now);
			// yyyyyy hat in diesem Modell die Konsequenz, dass, wenn jemand zu Hause bleibt, die andere Person alleine rumsitzt.  Somewhat plausible in public
			// transport; not plausible in restaurant.
			return;
		}

		// start tracking late as possible because of computational costs
		boolean trackingEnabled = iteration >= trackingAfterDay;

		Pair<MutableEpisimPerson, Double> group = removePersonFromGroups(container, personLeavingContainer, now);

		MutableEpisimPerson contactPerson = group.getKey();

		if (!personRelevantForTrackingOrInfectionDynamics(contactPerson, event, getRestrictions(), rnd)) {
			return;
		}

		// we have thrown the random numbers, so we can bail out in some cases if we are not tracking:
		if (!trackingEnabled) {
			if (personLeavingContainer.getDiseaseStatus() == DiseaseStatus.infectedButNotContagious) {
				return;
			}
			if (contactPerson.getDiseaseStatus() == DiseaseStatus.infectedButNotContagious) {
				return;
			}
			if (personLeavingContainer.getDiseaseStatus() == contactPerson.getDiseaseStatus()) {
				return;
			}
		} else if (!traceSusceptible && personLeavingContainer.getDiseaseStatus() == DiseaseStatus.susceptible
				&& contactPerson.getDiseaseStatus() == DiseaseStatus.susceptible)
			return;

		String leavingPersonsActivity = event.getActivity().getContainerName();

		// TODO: does not store performed activities
		String otherPersonsActivity = event.getActivity().getContainerName();

		StringBuilder infectionType = getInfectionType(buffer, container, leavingPersonsActivity, otherPersonsActivity);

		// use joint time in group as time
		// TODO: this model does not support closing hours at the moment
		double jointTimeInContainer = now - group.getValue();

		log.debug("Contact of {} and {}, with {}", personLeavingContainer, contactPerson, jointTimeInContainer);

		if (jointTimeInContainer == 0) {
			return;
		}

		//forbid certain cross-activity interactions, keep track of contacts
		if (container.isFacility()) {
			//home can only interact with home, leisure or work
			if (infectionType.indexOf("home") >= 0 && infectionType.indexOf("leis") == -1 && infectionType.indexOf("work") == -1
					&& !(leavingPersonsActivity.startsWith("home") && otherPersonsActivity.startsWith("home"))) {
				// yyyyyy we need to move out of these string convention based rules in code.  kai, aug'20
				return;
			} else if (infectionType.indexOf("edu") >= 0 && infectionType.indexOf("work") == -1 && !(leavingPersonsActivity.startsWith("edu") && otherPersonsActivity.startsWith("edu"))) {
				//edu can only interact with work or edu
				// yyyyyy we need to move out of these string convention based rules in code.  kai, aug'20
				return;
			}
			if (trackingEnabled) {
				trackContactPerson(personLeavingContainer, contactPerson, now, jointTimeInContainer, infectionType);
			}

			// Only a subset of contacts are reported at the moment
			// tracking has to be enabled to report more contacts
			reporting.reportContact(now, personLeavingContainer, contactPerson, container, infectionType, jointTimeInContainer, event.getNumberOfContacts());
		}

		if (!AbstractContactModel.personsCanInfectEachOther(personLeavingContainer, contactPerson)) {
			return;
		}

		// person can only infect others 4 days after being contagious
		if ((personLeavingContainer.hadDiseaseStatus(DiseaseStatus.contagious) &&
				personLeavingContainer.daysSince(DiseaseStatus.contagious, iteration) > 4)
				|| (contactPerson.hadDiseaseStatus(DiseaseStatus.contagious) &&
				contactPerson.daysSince(DiseaseStatus.contagious, iteration) > 4))
			return;

		if (jointTimeInContainer < 0 || jointTimeInContainer > 86400 * 7) {
			log.warn(jointTimeInContainer);
			throw new IllegalStateException("joint time in container is not plausible for personLeavingContainer=" + personLeavingContainer.getPersonId() + " and contactPerson=" + contactPerson.getPersonId() + ". Joint time is=" + jointTimeInContainer);
		}

		EpisimConfigGroup.InfectionParams leavingParams = getInfectionParams(container, personLeavingContainer, leavingPersonsActivity);

		// activity params of the contact person and leaving person
		EpisimConfigGroup.InfectionParams contactParams = getInfectionParams(container, contactPerson, otherPersonsActivity);

		// need to differentiate which person might be the infector
		if (personLeavingContainer.getDiseaseStatus() == DiseaseStatus.susceptible) {

			double prob = infectionModel.calcInfectionProbability(personLeavingContainer, contactPerson, getRestrictions(),
					leavingParams, contactParams, jointTimeInContainer);
			if (rnd.nextDouble() < prob)
				infectPerson(personLeavingContainer, contactPerson, now, infectionType, container, event.getNumberOfContacts());

		} else {
			double prob = infectionModel.calcInfectionProbability(contactPerson, personLeavingContainer, getRestrictions(),
					contactParams, leavingParams, jointTimeInContainer);

			if (rnd.nextDouble() < prob)
				infectPerson(contactPerson, personLeavingContainer, now, infectionType, container, event.getNumberOfContacts());
		}
//		}
	}

	/**
	 * Remove a person from groups and form new groups.
	 *
	 * @return contact person if person was in group.
	 */
	private Pair<MutableEpisimPerson, Double> removePersonFromGroups(EpisimContainer container, MutableEpisimPerson personLeavingContainer, double time) {
		if (singlePersons.get(container) == personLeavingContainer) {
			singlePersons.remove(container);
			return null;
		} else {
			Group group = findGroup(container, personLeavingContainer);

			// might happen during init when person leaves first
			if (group == null)
				return null;

			// other person will be single person
			MutableEpisimPerson leftOverPerson = group.remove(personLeavingContainer);
			if (!singlePersons.containsKey(container)) {
				singlePersons.put(container, leftOverPerson);
			} else {

				// single person and left over person will form a new group
				groups.get(container)
						.add(Group.of(leftOverPerson, singlePersons.get(container), time));

				singlePersons.remove(container);
			}

			groups.get(container).remove(group);

			return Pair.of(leftOverPerson, group.time);
		}
	}

	/**
	 * A group of two persons and time when the group was formed.
	 */
	private static final class Group {

		private final MutableEpisimPerson a;
		private final MutableEpisimPerson b;
		private final double time;

		private Group(MutableEpisimPerson a, MutableEpisimPerson b, double time) {
			this.a = a;
			this.b = b;
			this.time = time;
		}

		private static Group of(MutableEpisimPerson a, MutableEpisimPerson b, double time) {
			return new Group(a, b, time);
		}

		public boolean contains(MutableEpisimPerson person) {
			return a == person || b == person;
		}

		/**
		 * Return the left over person.
		 */
		public MutableEpisimPerson remove(MutableEpisimPerson p) {
			if (p == a) return b;
			else if (p == b) return a;
			throw new IllegalStateException("Leaving person not in group.");
		}
	}

}
