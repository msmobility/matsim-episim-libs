package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.episim.EpisimConfigGroup;


public interface PersonEntersContainerEvent extends HasPersonId {

	/**
	 * Id of the container left.
	 */
	EpisimContainer getContainer();

	/**
	 * Activity this person performed.
	 */
	EpisimConfigGroup.InfectionParams getActivity();

	/**
	 * Time in seconds since start of the day when this person entered the facility.
	 */
	int getEnterTime();

	/**
	 * Creates a new instances of a person leave event.
	 */
	static PersonEntersContainerEvent newInstance(Id<Person> person, EpisimContainer container,
												  EpisimConfigGroup.InfectionParams param, int enterTime) {
		return new PersonEntersContainerEventImpl(person, container, param, enterTime);
	}
}
