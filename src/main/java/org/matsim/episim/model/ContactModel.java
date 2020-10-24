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
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.MutableEpisimPerson;
import org.matsim.episim.InfectionEventHandler;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.PersonEntersContainerEvent;
import org.matsim.episim.data.PersonLeavesContainerEvent;
import org.matsim.episim.policy.Restriction;

import java.util.Map;

/**
 * This class models the contacts of persons staying in the same place for a certain time.
 */
public interface ContactModel {

	/**
	 * This method is called when a persons leave a container at {@code now}.
	 */
	void infectionDynamicsContainer(PersonLeavesContainerEvent event, double now);

	/**
	 * Called when a container is entered by a person.
	 */
	default void notifyEnterContainer(PersonEntersContainerEvent event, double now) { }

	/**
	 * Set the current iteration and restrictions in place.
	 */
	void setIteration(int iteration, Map<Id<Person>, MutableEpisimPerson> persons, Map<String, Restriction> restrictions);

}
