package org.matsim.episim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.PersonContact;
import org.matsim.episim.data.PersonLeavesContainerEvent;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * A mutable event, which depends on the current context.
 */
class MutablePersonLeavesContainerEvent implements PersonLeavesContainerEvent {

	private int now;
	private MutableEpisimPerson person;
	private MutableEpisimContainer container;

	MutablePersonLeavesContainerEvent setContext(int now, MutableEpisimPerson person, MutableEpisimContainer container) {
		this.now = now;
		this.person = person;
		this.container = container;
		return this;
	}

	void reset() {
		this.now = -1;
		this.person = null;
		this.container = null;
	}

	public MutableEpisimPerson getPerson() {
		return person;
	}

	@Override
	public EpisimContainer getContainer() {
		return container;
	}

	@Override
	public EpisimConfigGroup.InfectionParams getActivity() {
		return person.getTrajectory().get(person.getCurrentPositionInTrajectory()).params;
	}

	@Nullable
	@Override
	public EpisimConfigGroup.InfectionParams getPrevActivity() {
		if (person.getCurrentPositionInTrajectory() != 0) {
			return person.getTrajectory().get(person.getCurrentPositionInTrajectory() - 1).params;
		}

		return null;
	}

	@Nullable
	@Override
	public EpisimConfigGroup.InfectionParams getNextActivity() {
		return person.getTrajectory().get(person.getCurrentPositionInTrajectory() + 1).params;
	}

	@Override
	public int getTime() {
		return now;
	}

	@Override
	public int getEnterTime() {
		return (int) container.getContainerEnteringTime(person.getPersonId());
	}

	@Override
	public int getNumberOfContacts() {
		return container.getPersons().size() - 1;
	}

	@Override
	public Iterator<PersonContact> iterator() {
		// TODO
		// TODO ##############################
		return null;
	}

	@Override
	public PersonContact getContact(int index) {
		// TODO
		return null;
	}

	@Override
	public Id<Person> getPersonId() {
		return person.getPersonId();
	}
}
