package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;

public interface EpisimContainer {

	Id<EpisimContainer> getId();

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

}
