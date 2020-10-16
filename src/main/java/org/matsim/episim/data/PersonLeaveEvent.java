package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.facilities.ActivityFacility;

import java.util.List;

/**
 * Low-level event of a person leaving a facility.
 * Instances of this class must not be stored, only consumed!
 */
public interface PersonLeaveEvent extends Iterable<PersonContact> {

	/**
	 * Id of the leaving person.
	 */
	int getPersonId();

	/**
	 * Id of the facility left.
	 */
	int getFacilityId();

	/**
	 * Whether this container is a vehicle.
	 */
	boolean isInVehicle();

	/**
	 * Activity this person performed.
	 */
	EpisimConfigGroup.InfectionParams getActivity();

	/**
	 * Time in seconds since start of the day, when this person left.
	 */
	int getLeaveTime();

	/**
	 * Time in seconds since start of the day when this person entered the facility.
	 */
	int getEnterTime();

	/**
	 * Number of contacts possible in this container.
	 */
	int getNumberOfContacts();

	/**
	 * Creates a new instances of a person leave event.
	 */
	static PersonLeaveEvent newInstance(Id<Person> personId, Id<ActivityFacility> facilityId,
										boolean isVehicle, EpisimConfigGroup.InfectionParams param,
										int leaveTime, int enterTime, List<PersonContact> contacts) {
		return new PersonLeaveEventImpl(personId.index(), facilityId.index(), isVehicle, param, leaveTime, enterTime, contacts);
	}

}
