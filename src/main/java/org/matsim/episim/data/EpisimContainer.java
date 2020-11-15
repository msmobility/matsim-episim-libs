package org.matsim.episim.data;

import org.apache.commons.lang3.NotImplementedException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimUtils;
import org.matsim.episim.MutableEpisimPerson;

import java.io.*;
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

	/**
	 * Write container data to output stream.
	 */
	default void write(DataOutput out) throws IOException {
		EpisimUtils.writeChars(out, getContainerId().toString());
		out.writeBoolean(isVehicle());
		out.writeInt(getMaxGroupSize());
		out.writeInt(getTotalUsers());
		out.writeInt(getTypicalCapacity());
		out.writeDouble(getNumSpaces());
	}

	/**
	 * Read container from input stream
	 */
	static EpisimContainer read(DataInput in) throws IOException {

		Id<EpisimContainer> id = Id.create(EpisimUtils.readChars(in), EpisimContainer.class);
		boolean isVehicle = in.readBoolean();
		int maxGroupSize = in.readInt();
		int totalUsers = in.readInt();
		int typicalCapacity = in.readInt();
		double numSpaces = in.readDouble();

		return new EpisimContainerImpl(id, isVehicle, maxGroupSize, totalUsers, typicalCapacity, numSpaces);
	}

	/**
	 * Whether this container is a facility.
	 */
	default boolean isFacility() {
		return !isVehicle();
	}
}
