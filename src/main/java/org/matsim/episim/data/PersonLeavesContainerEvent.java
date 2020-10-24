package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.episim.EpisimConfigGroup;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Low-level event of a person leaving a container (facility or vehicle).
 * Instances of this class must not be stored, only consumed!
 */
public interface PersonLeavesContainerEvent extends EpisimEvent, Iterable<PersonContact> {

	/**
	 * Id of the container left.
	 */
	EpisimContainer getContainer();

	/**
	 * Activity this person performed.
	 */
	EpisimConfigGroup.InfectionParams getActivity();

	/**
	 * Previous activity of this person. Null if there is none.
	 */
	@Nullable
	EpisimConfigGroup.InfectionParams getPrevActivity();

	/**
	 * Next activity of this person. Null if there is none.
	 */
	@Nullable
	EpisimConfigGroup.InfectionParams getNextActivity();

	/**
	 * Time in seconds since start of the day, when this person left.
	 */
	int getTime();

	/**
	 * Time in seconds since start of the day when this person entered the facility.
	 */
	int getEnterTime();

	/**
	 * Number of contacts possible in this container.
	 */
	int getNumberOfContacts();

	/**
	 * Get a single contact by its index, going from 0 to {@link #getNumberOfContacts()}.
	 */
	PersonContact getContact(int index);

	/**
	 * Creates a new instances of a person leave event.
	 */
	static PersonLeavesContainerEvent newInstance(Id<Person> person, EpisimContainer container,
												  EpisimConfigGroup.InfectionParams param,
												  int leaveTime, int enterTime, List<PersonContact> contacts) {
		return new PersonLeavesContainerEventImpl(person, container, param, leaveTime, enterTime, contacts);
	}

}
