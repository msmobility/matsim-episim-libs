package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.Set;

/**
 * Class responsible for providing episim specific events to the {@link org.matsim.episim.InfectionEventHandler}.
 */
public interface EpisimEventProvider {

	/**
	 * Called before the simulation starts.
	 */
	void init();

	/**
	 * All persons relevant for this provider. The provider is supposed to create these during init phase.
	 */
	Collection<Id<Person>> getPersonIds();

	/**
	 * All containers that have been created and that are relevant for the simulation.
	 */
	Collection<EpisimContainer> getContainer();

	/**
	 * Returns an iterable that provides all events for given day.
	 *
	 * @param day the day to simulate
	 */
	Iterable<EpisimEvent> forDay(DayOfWeek day);

}
