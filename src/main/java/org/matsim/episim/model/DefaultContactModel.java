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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.*;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import org.matsim.episim.data.DiseaseStatus;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.PersonContact;
import org.matsim.episim.data.PersonLeavesContainerEvent;

/**
 * Default contact model executed, when a person ends his activity.
 * Infections probabilities calculations are delegated to a {@link InfectionModel}.
 */
public final class DefaultContactModel extends AbstractContactModel {

	private static final Logger log = LogManager.getLogger(DefaultContactModel.class);

	/**
	 * Flag to enable tracking, which is considerably slower.
	 */
	private final int trackingAfterDay;

	/**
	 * In order to avoid recreating a the list of other persons in the container every time it is stored as instance variable.
	 */
	private final IntList contacts = new IntArrayList();
	/**
	 * This buffer is used to store the infection type.
	 */
	private final StringBuilder buffer = new StringBuilder();

	@Inject
	/* package */
	DefaultContactModel(SplittableRandom rnd, Config config,
						EpisimReporting reporting, InfectionModel infectionModel) {
		// (make injected constructor non-public so that arguments can be changed without repercussions.  kai, jun'20)
		super(rnd, config, infectionModel, reporting);
		this.trackingAfterDay = ConfigUtils.addOrGetModule(config, TracingConfigGroup.class).getPutTraceablePersonsInQuarantineAfterDay();
	}

	@Override
	public void infectionDynamicsContainer(PersonLeavesContainerEvent event, double now) {

		EpisimContainer container = event.getContainer();
		MutableEpisimPerson personLeavingContainer = persons.get(event.getPersonId());

		// no infection possible if there is only one person
		if (iteration == 0 || event.getNumberOfContacts() == 0) {
			return;
		}

		if (!personRelevantForTrackingOrInfectionDynamics(personLeavingContainer, event, getRestrictions(), rnd)) {
			return;
		}

		// start tracking late as possible because of computational costs
		boolean trackingEnabled = iteration >= trackingAfterDay;

		EpisimConfigGroup.InfectionParams leavingParams = null;

		for (int i = 0; i < event.getNumberOfContacts(); i++) {
			contacts.add(i);
		}

		// For the time being, will just assume that the first 10 persons are the ones we interact with.  Note that because of
		// shuffle, those are 10 different persons every day.

		// persons are scaled to number of agents with sample size, but at least 3 for the small development scenarios
//		int contactWith = Math.min(otherPersonsInContainer.size(), Math.max((int) (episimConfig.getSampleSize() * 10), 3));
		int contactWith = Math.min(contacts.size(), (int)episimConfig.getMaxContacts());
		for (int ii = 0; ii < contactWith; ii++) {

			// we are essentially looking at the situation when the person leaves the container.  Interactions with other persons who have
			// already left the container were treated then.  In consequence, we have some "circle of persons around us" (yyyy which should
			//  depend on the density), and then a probability of infection in either direction.

			// Draw the contact person and remove it -> we don't want to draw it multiple times
			PersonContact contact = event.getContact(contacts.removeInt(rnd.nextInt(contacts.size())));
			MutableEpisimPerson contactPerson = persons.get(contact.getContactPerson());

			if (!personRelevantForTrackingOrInfectionDynamics(contactPerson, event, getRestrictions(), rnd)) {
				continue;
			}

			// we have thrown the random numbers, so we can bail out in some cases if we are not tracking:
			if (!trackingEnabled) {
				if (personLeavingContainer.getDiseaseStatus() == DiseaseStatus.infectedButNotContagious) {
					continue;
				}
				if (contactPerson.getDiseaseStatus() == DiseaseStatus.infectedButNotContagious) {
					continue;
				}
				if (personLeavingContainer.getDiseaseStatus() == contactPerson.getDiseaseStatus()) {
					continue;
				}
			}

			String leavingPersonsActivity = event.getActivity().getContainerName();
			String otherPersonsActivity = contact.getContactPersonActivity().getContainerName();

			StringBuilder infectionType = getInfectionType(buffer, container, leavingPersonsActivity, otherPersonsActivity);

			double jointTimeInContainer = calculateJointTimeInContainer(now, contact);

			//forbid certain cross-activity interactions, keep track of contacts
			if (!container.isVehicle()) {
				//home can only interact with home, leisure or work
				if (infectionType.indexOf("home") >= 0 && infectionType.indexOf("leis") == -1 && infectionType.indexOf("work") == -1
						&& !(leavingPersonsActivity.startsWith("home") && otherPersonsActivity.startsWith("home"))) {
					continue;
				} else if (infectionType.indexOf("edu") >= 0 && infectionType.indexOf("work") == -1 && !(leavingPersonsActivity.startsWith("edu") && otherPersonsActivity.startsWith("edu"))) {
					//edu can only interact with work or edu
					continue;
				}
				if (trackingEnabled) {
					trackContactPerson(personLeavingContainer, contactPerson, now, jointTimeInContainer, infectionType);
				}

				// Only a subset of contacts are reported at the moment
				// tracking has to be enabled to report more contacts
				reporting.reportContact(now, personLeavingContainer, contactPerson, container, infectionType, jointTimeInContainer, event.getNumberOfContacts());
			}

			if (!AbstractContactModel.personsCanInfectEachOther(personLeavingContainer, contactPerson)) {
				continue;
			}

			// person can only infect others 4 days after being contagious
			if ((personLeavingContainer.hadDiseaseStatus(DiseaseStatus.contagious) &&
					personLeavingContainer.daysSince(DiseaseStatus.contagious, iteration) > 4)
					|| (contactPerson.hadDiseaseStatus(DiseaseStatus.contagious) &&
					contactPerson.daysSince(DiseaseStatus.contagious, iteration) > 4))
				continue;

			if (jointTimeInContainer < 0 || jointTimeInContainer > 86400 * 7) {
				log.warn(now);
				throw new IllegalStateException("joint time in container is not plausible for personLeavingContainer=" + personLeavingContainer.getPersonId() + " and contactPerson=" + contactPerson.getPersonId() + ". Joint time is=" + jointTimeInContainer);
			}

			// Parameter will only be retrieved one time
			if (leavingParams == null)
				leavingParams = getInfectionParams(container, personLeavingContainer, leavingPersonsActivity);

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
		}

		// Clear cached container
		contacts.clear();
	}


}
