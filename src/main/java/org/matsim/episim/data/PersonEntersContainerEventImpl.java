package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;

/**
 * Simple implementation for person enters container event.
 */
public class PersonEntersContainerEventImpl implements PersonEntersContainerEvent {
	private final Id<Person> personId;
	private final EpisimContainer container;
	private final EpisimConfigGroup.InfectionParams param;
	private final int enterTime;

	public PersonEntersContainerEventImpl(Id<Person> person, EpisimContainer container, EpisimConfigGroup.InfectionParams param, int enterTime) {
		this.personId = person;
		this.container = container;
		this.param = param;
		this.enterTime = enterTime;
	}

	@Override
	public EpisimContainer getContainer() {
		return container;
	}

	@Override
	public EpisimConfigGroup.InfectionParams getActivity() {
		return param;
	}

	@Override
	public int getTime() {
		return enterTime;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}
}
