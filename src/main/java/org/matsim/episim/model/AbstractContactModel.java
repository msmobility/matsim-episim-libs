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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.episim.*;
import org.matsim.episim.data.*;
import org.matsim.episim.policy.Restriction;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.util.Map;
import java.util.SplittableRandom;


/**
 * Base implementation for interactions of persons during activities.
 */
public abstract class AbstractContactModel implements ContactModel {
	public static final String QUARANTINE_HOME = "quarantine_home";

	protected final Scenario scenario = null;
	protected final SplittableRandom rnd;
	protected final EpisimConfigGroup episimConfig;
	protected final EpisimReporting reporting;

	/**
	 * Infections parameter instances for re-use. These are params that are always needed independent of the scenario.
	 */
	protected final MutableEpisimPerson.Activity trParams;
	/**
	 * Home quarantine infection param.
	 */
	protected final MutableEpisimPerson.Activity qhParams;
	/**
	 * See {@link TracingConfigGroup#getMinDuration()}
	 */
	protected final double trackingMinDuration;

	/**
	 * Infection probability calculation.
	 */
	protected final InfectionModel infectionModel;

	/**
	 * Map of all persons.
	 */
	protected Map<Id<Person>, MutableEpisimPerson> persons;

	protected int iteration;


	private Map<String, Restriction> restrictions;


	AbstractContactModel(SplittableRandom rnd, Config config, InfectionModel infectionModel, EpisimReporting reporting) {
		this.rnd = rnd;
		this.episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		this.infectionModel = infectionModel;
		this.reporting = reporting;
		this.trParams = new MutableEpisimPerson.Activity("tr", episimConfig.selectInfectionParams("tr"));
		this.qhParams = new MutableEpisimPerson.Activity(QUARANTINE_HOME, episimConfig.selectInfectionParams(QUARANTINE_HOME));
		this.trackingMinDuration = ConfigUtils.addOrGetModule(config, TracingConfigGroup.class).getMinDuration();
	}

	private static boolean hasDiseaseStatusRelevantForInfectionDynamics(MutableEpisimPerson personWrapper) {
		switch (personWrapper.getDiseaseStatus()) {
			case susceptible:
			case contagious:
			case showingSymptoms:
				return true;

			case infectedButNotContagious:
			case recovered:
			case seriouslySick: // assume is in hospital
			case critical:
			case seriouslySickAfterCritical:
				return false;

			default:
				throw new IllegalStateException("Unexpected value: " + personWrapper.getDiseaseStatus());
		}
	}

	/**
	 * This method checks whether person1 and person2 have relevant disease status for infection dynamics.
	 * If not or if both have the same disease status, the return value is false.
	 */
	static boolean personsCanInfectEachOther(MutableEpisimPerson person1, MutableEpisimPerson person2) {
		if (person1.getDiseaseStatus() == person2.getDiseaseStatus()) return false;
		// at least one of the persons must be susceptible
		if (person1.getDiseaseStatus() != DiseaseStatus.susceptible && person2.getDiseaseStatus() != DiseaseStatus.susceptible)
			return false;
		return (hasDiseaseStatusRelevantForInfectionDynamics(person1) && hasDiseaseStatusRelevantForInfectionDynamics(person2));
	}

	/**
	 * Attention: In order to re-use the underlying object, this function returns a buffer.
	 * Be aware that the old result will be overwritten, when the function is called multiple times.
	 */
	protected static StringBuilder getInfectionType(StringBuilder buffer, EpisimContainer container, String leavingPersonsActivity,
													String otherPersonsActivity) {
		buffer.setLength(0);
		if (!container.isVehicle()) {
			buffer.append(leavingPersonsActivity).append("_").append(otherPersonsActivity);
		} else  {
			buffer.append("pt");
		}
		return buffer;
	}

	/**
	 * Get the relevant infection parameter based on container and activity and person.
	 */
	protected EpisimConfigGroup.InfectionParams getInfectionParams(EpisimContainer container, MutableEpisimPerson person, String activity) {
		if (container.isVehicle()) {
			return trParams.params;
		} else  {
			EpisimConfigGroup.InfectionParams params = episimConfig.selectInfectionParams(activity);

			// Select different infection params for home quarantined persons
			if (person.getQuarantineStatus() == QuarantineStatus.atHome && params.getContainerName().equals("home")) {
				return qhParams.params;
			}

			return params;
		}
	}

	protected void trackContactPerson(MutableEpisimPerson personLeavingContainer, MutableEpisimPerson otherPerson, double now, double jointTimeInContainer,
									  StringBuilder infectionType) {

		// Don't track certain activities
		if (infectionType.indexOf("pt") >= 0 || infectionType.indexOf("shop") >= 0) {
			return;
		}

		// don't track below threshold
		if (jointTimeInContainer < trackingMinDuration) {
			return;
		}

		personLeavingContainer.addTraceableContactPerson(otherPerson, now);
		otherPerson.addTraceableContactPerson(personLeavingContainer, now);
	}

	private boolean activityRelevantForInfectionDynamics(MutableEpisimPerson person, EpisimContainer container, EpisimConfigGroup.InfectionParams act,
														 Map<String, Restriction> restrictions, SplittableRandom rnd) {

		// Check if person is home quarantined
		if (person.getQuarantineStatus() == QuarantineStatus.atHome && !act.getContainerName().startsWith("home"))
			return false;


		// enforce max group sizes
		Restriction r = restrictions.get(act.getContainerName());
		if (r.getMaxGroupSize() != null && r.getMaxGroupSize() > -1 && container.getMaxGroupSize() > 0 &&
				container.getMaxGroupSize() > r.getMaxGroupSize())
			return false;

		if (r.isClosed(container.getContainerId()))
			return false;

		return actIsRelevant(act, restrictions, rnd);
	}

	private boolean actIsRelevant(EpisimConfigGroup.InfectionParams act, Map<String, Restriction> restrictions, SplittableRandom rnd) {

		Restriction r = restrictions.get(act.getContainerName());
		// avoid use of rnd if outcome is known beforehand
		if (r.getRemainingFraction() == 1)
			return true;
		if (r.getRemainingFraction() == 0)
			return false;

		return rnd.nextDouble() < r.getRemainingFraction();

	}

	private boolean tripRelevantForInfectionDynamics(MutableEpisimPerson person, PersonLeavesContainerEvent event, Map<String, Restriction> restrictions, SplittableRandom rnd) {

		if (person.getQuarantineStatus() != QuarantineStatus.no)
			return false;

		// last activity is only considered if present
		return actIsRelevant(trParams.params, restrictions, rnd) && (event.getNextActivity() == null || actIsRelevant(event.getNextActivity(), restrictions, rnd))
				&& (event.getPrevActivity() == null || actIsRelevant(event.getPrevActivity(), restrictions, rnd));

	}

	/**
	 * Checks whether person is relevant for tracking or for infection dynamics.  Currently, "relevant for infection dynamics" is a subset of "relevant for
	 * tracking".  However, I am not sure if this will always be the case.  kai, apr'20
	 *
	 * @noinspection BooleanMethodIsAlwaysInverted
	 */
	protected final boolean personRelevantForTrackingOrInfectionDynamics(MutableEpisimPerson person, PersonLeavesContainerEvent event,
																		 Map<String, Restriction> restrictions, SplittableRandom rnd) {

		return personHasRelevantStatus(person) && checkPersonInContainer(person, event, restrictions, rnd);
	}

	protected final boolean personHasRelevantStatus(MutableEpisimPerson person) {
		// Infected but not contagious persons are considered additionally
		return hasDiseaseStatusRelevantForInfectionDynamics(person) ||
				person.getDiseaseStatus() == DiseaseStatus.infectedButNotContagious;
	}

	/**
	 * Checks whether a person would be present in the container.
	 */
	protected final boolean checkPersonInContainer(MutableEpisimPerson person, PersonLeavesContainerEvent event,
												   Map<String, Restriction> restrictions, SplittableRandom rnd) {
		if (person.getQuarantineStatus() == QuarantineStatus.full) {
			return false;
		}

		if (!event.getContainer().isVehicle() && activityRelevantForInfectionDynamics(person, event.getContainer(), event.getActivity(), restrictions, rnd)) {
			return true;
		}
		return event.getContainer().isVehicle() && tripRelevantForInfectionDynamics(person, event, restrictions, rnd);
	}

	/**
	 * Calculate the joint time persons have been in a container.
	 * This takes possible closing hours into account.
	 */
	protected double calculateJointTimeInContainer(double now, PersonContact contact) {
		Restriction r = getRestrictions().get(contact.getContactPersonActivity().getContainerName());

		double jointTime = contact.getDuration();
		/* TODO curfew hours
		// no closing hour set
		if (r.getClosingHours() == null)
			return now - Math.max(containerEnterTimeOfPersonLeaving, containerEnterTimeOfOtherPerson);

		// entering times are shifted to the past if the facility was closed
		// now (leave time) is shifted to be earlier if the facility closed already at earlier point
		double jointTime = r.adjustByClosingHour(now, false) - Math.max(
				r.adjustByClosingHour(containerEnterTimeOfPersonLeaving, true),
				r.adjustByClosingHour(containerEnterTimeOfOtherPerson, true)
		);

		// joint time can now be negative and will be set to 0
		return jointTime > 0 ? jointTime : 0;
		 */

		if (jointTime < 0 || jointTime > 86400 * 7) {
			throw new IllegalStateException("joint time in container is not plausible for contactPerson=" + contact.getContactPerson() + ". Joint time is=" + contact.getDuration());
		}

		return jointTime;
	}


	@Override
	public void setIteration(int iteration, Map<Id<Person>, MutableEpisimPerson> persons, Map<String, Restriction> restrictions) {
		this.iteration = iteration;
		this.restrictions = restrictions;
		this.persons = persons;
		this.infectionModel.setIteration(iteration);
	}

	/**
	 * Sets the infection status of a person and reports the event.
	 */
	protected void infectPerson(MutableEpisimPerson personWrapper, MutableEpisimPerson infector, double now, StringBuilder infectionType,
								EpisimContainer container, int groupSize) {

		if (personWrapper.getDiseaseStatus() != DiseaseStatus.susceptible) {
			throw new IllegalStateException("Person to be infected is not susceptible. Status is=" + personWrapper.getDiseaseStatus());
		}
		if (infector.getDiseaseStatus() != DiseaseStatus.contagious && infector.getDiseaseStatus() != DiseaseStatus.showingSymptoms) {
			throw new IllegalStateException("Infector is not contagious. Status is=" + infector.getDiseaseStatus());
		}
		if (personWrapper.getQuarantineStatus() == QuarantineStatus.full) {
			throw new IllegalStateException("Person to be infected is in full quarantine.");
		}
		if (infector.getQuarantineStatus() == QuarantineStatus.full) {
			throw new IllegalStateException("Infector is in ful quarantine.");
		}

		// TODO: during iteration persons can get infected after 24h
		// this can lead to strange effects / ordering of events, because it is assumed one iteration is one day
		// now is overwritten to be at the end of day
		if (now >= EpisimUtils.getCorrectedTime(episimConfig.getStartOffset(), 24 * 60 * 60, iteration)) {
			now = EpisimUtils.getCorrectedTime(episimConfig.getStartOffset(), 24 * 60 * 60 - 1, iteration);
		}

		String infType = infectionType.toString();

		reporting.reportInfection(personWrapper, infector, now, infType, container, groupSize);
		personWrapper.setDiseaseStatus(now, DiseaseStatus.infectedButNotContagious);
		personWrapper.setInfectionContainer(container);
		personWrapper.setInfectionType(infType);

		// TODO: Currently not in use, is it still needed?
		// Necessary for the otfvis visualization (although it is unfortunately not working).  kai, apr'20
		if (scenario != null) {
			final Person person = PopulationUtils.findPerson(personWrapper.getPersonId(), scenario);
			if (person != null) {
				person.getAttributes().putAttribute(AgentSnapshotInfo.marker, true);
			}
		}
	}

	public Map<String, Restriction> getRestrictions() {
		return restrictions;
	}

}
