package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimUtils;
import org.matsim.episim.MutableEpisimPerson;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

/**
 * Basic implementation for a container (data class).
 */
class EpisimContainerImpl implements EpisimContainer {

	private final Id<EpisimContainer> id;
	private final boolean isVehicle;
	private final int maxGroupSize;
	private final int totalUsers;
	private final int typicalCapacity;
	private final double numSpaces;

	EpisimContainerImpl(Id<EpisimContainer> id, boolean isVehicle, int maxGroupSize, int totalUsers, int typicalCapacity, double numSpaces) {
		this.id = id;
		this.isVehicle = isVehicle;
		this.maxGroupSize = maxGroupSize;
		this.totalUsers = totalUsers;
		this.typicalCapacity = typicalCapacity;
		this.numSpaces = numSpaces;
	}

	@Override
	public Id<EpisimContainer> getContainerId() {
		return id;
	}

	@Override
	public boolean isVehicle() {
		return isVehicle;
	}

	@Override
	public int getMaxGroupSize() {
		return maxGroupSize;
	}

	@Override
	public int getTotalUsers() {
		return totalUsers;
	}

	@Override
	public int getTypicalCapacity() {
		return typicalCapacity;
	}

	@Override
	public double getNumSpaces() {
		return numSpaces;
	}

}
