package org.matsim.episim.model;

import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.MutableEpisimPerson;
import org.matsim.episim.policy.Restriction;

/**
 * Model which decides which mask a person is wearing during activity.
 */
public interface FaceMaskModel {

	/**
	 * Return the mask a person is wearing for specific activity.
	 */
	FaceMask getWornMask(MutableEpisimPerson person, EpisimConfigGroup.InfectionParams act, Restriction restriction);

}
