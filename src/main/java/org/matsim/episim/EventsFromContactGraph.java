package org.matsim.episim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.EpisimEventProvider;
import org.matsim.episim.data.PersonLeavesContainerEvent;

import java.time.DayOfWeek;
import java.util.Map;

public class EventsFromContactGraph implements EpisimEventProvider {
	@Override
	public void init() {

	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public Map<Id<Person>, MutableEpisimPerson> getPersons() {
		return null;
	}

	@Override
	public Map<Id<EpisimContainer>, ? extends EpisimContainer> getContainer() {
		return null;
	}

	@Override
	public Iterable<PersonLeavesContainerEvent> forDay(DayOfWeek day) {
		return null;
	}
}
