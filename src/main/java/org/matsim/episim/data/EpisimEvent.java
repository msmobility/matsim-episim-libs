package org.matsim.episim.data;

import org.matsim.core.api.internal.HasPersonId;

/**
 * Interface for episim specific events.
 */
public interface EpisimEvent extends HasPersonId {
	/**
	 * Seconds since start of day when this event happens.
	 */
	int getTime();
}
