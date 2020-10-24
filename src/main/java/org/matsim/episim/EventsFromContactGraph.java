package org.matsim.episim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.EpisimEvent;
import org.matsim.episim.data.EpisimEventProvider;

import java.time.DayOfWeek;
import java.util.Set;

public class EventsFromContactGraph implements EpisimEventProvider {
	@Override
	public void init() {

	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public Set<Id<Person>> getPersonIds() {
		return null;
	}

	@Override
	public Set<EpisimContainer> getContainer() {
		return null;
	}

	@Override
	public Iterable<EpisimEvent> forDay(DayOfWeek day) {
		return null;
	}
}
