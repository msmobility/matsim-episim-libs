package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;

/**
 * Object representing the contact with a different person.
 * This class only contains partial information, i.e. as few as possible.
 * vInstances of this class must not be stored, only consumed!
 */
public interface PersonContact {

	/**
	 * Id of the contact person.
	 */
	EpisimPerson getContactPerson();

	/**
	 * Activity that the other person performed.
	 */
	EpisimConfigGroup.InfectionParams getContactPersonActivity();

	/**
	 * Offset in seconds since entering the container when this contact can happen.
	 */
	int getOffset();

	/**
	 * Duration of the contact in seconds.
	 */
	int getDuration();

	/**
	 * Creates a new instance with simple representation.
	 */
	static PersonContact newInstance(EpisimPerson contactPerson, EpisimConfigGroup.InfectionParams params, int offset, int duration) {
		return new PersonContactImpl(contactPerson, params, offset, duration);
	}

}
