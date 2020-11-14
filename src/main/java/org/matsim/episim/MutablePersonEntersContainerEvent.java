package org.matsim.episim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.PersonEntersContainerEvent;

/**
 * Person event with mutable fields.
 */
final class MutablePersonEntersContainerEvent implements PersonEntersContainerEvent {

	private int now;
	private MutableEpisimPerson person;
	private MutableEpisimContainer container;
	private EpisimConfigGroup.InfectionParams actType;

	MutablePersonEntersContainerEvent setContext(int now, MutableEpisimPerson person, MutableEpisimContainer container,
												 EpisimConfigGroup.InfectionParams actType) {
		this.now = now;
		this.person = person;
		this.container = container;
		this.actType = actType;
		return this;
	}

	@Override
	public EpisimContainer getContainer() {
		return container;
	}

	@Override
	public EpisimConfigGroup.InfectionParams getActivity() {
		return actType != null ? actType : person.getTrajectory().get(person.getCurrentPositionInTrajectory()).params;
	}

	@Override
	public int getTime() {
		return now;
	}

	@Override
	public Id<Person> getPersonId() {
		return person.getPersonId();
	}

}
