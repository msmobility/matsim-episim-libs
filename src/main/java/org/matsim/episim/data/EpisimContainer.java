package org.matsim.episim.data;

import org.apache.commons.lang3.NotImplementedException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.MutableEpisimPerson;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public interface EpisimContainer {

	/**
	 * Id of this container.
	 */
	Id<EpisimContainer> getContainerId();

	/**
	 * Whether this is a vehicle.
	 */
	boolean isVehicle();

	/**
	 * The maximum number of persons simultaneously in this container. Negative if unknown.
	 * Already scaled with sampleSize.
	 */
	int getMaxGroupSize();

	/**
	 * The number of persons using this container over all days.
	 */
	int getTotalUsers();

	/**
	 * Typical number of persons that can fit into this container.
	 */
	int getTypicalCapacity();

	/**
	 * Number of distinct spaces in this facility. May be relevant for certain contact models.
	 */
	double getNumSpaces();

	default void write(ObjectOutput out) throws IOException {
		throw new NotImplementedException("Not implemented");
	}

	default void read(ObjectInput in, Map<Id<Person>, MutableEpisimPerson> persons) throws IOException {
		throw new NotImplementedException("Not implemented");
	}

	/**
	 * Whether this container is a facility.
	 */
	default boolean isFacility() {
		return !isVehicle();
	}
}
