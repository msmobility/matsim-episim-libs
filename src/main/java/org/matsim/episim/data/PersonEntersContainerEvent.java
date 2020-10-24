package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.episim.EpisimConfigGroup;


public interface PersonEntersContainerEvent extends EpisimEvent {

	/**
	 * Container the person is entering.
	 */
	EpisimContainer getContainer();

	/**
	 * Activity this person performed.
	 */
	EpisimConfigGroup.InfectionParams getActivity();

	/**
	 * Creates a new instances of a person leave event.
	 */
	static PersonEntersContainerEvent newInstance(Id<Person> person, EpisimContainer container,
												  EpisimConfigGroup.InfectionParams param, int enterTime) {
		return new PersonEntersContainerEventImpl(person, container, param, enterTime);
	}
}
