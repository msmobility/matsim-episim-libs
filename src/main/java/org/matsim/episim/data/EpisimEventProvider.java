package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.MutableEpisimPerson;

import java.time.DayOfWeek;
import java.util.Map;

/**
 * Class responsible for providing episim specific events to the {@link org.matsim.episim.InfectionEventHandler}.
 */
public interface EpisimEventProvider {

	/**
	 * Called before the simulation starts.
	 */
	void init();

	/**
	 * Called before each iteration.
	 */
	void reset(int iteration);

	/**
	 * All persons relevant for this provider. The provider is supposed to create these during init phase.
	 */
	Map<Id<Person>, MutableEpisimPerson> getPersons();

	/**
	 * All containers that have been created and that are relevant for the simulation.
	 */
	Map<Id<EpisimContainer>, ? extends EpisimContainer> getContainer();

	/**
	 * Returns an iterable that provides all events for given day.
	 *
	 * @param day the day to simulate
	 */
	Iterable<PersonLeavesContainerEvent> forDay(DayOfWeek day);

}
