package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * Basic implementation of the person leave event.
 */
class PersonLeavesContainerEventImpl implements PersonLeavesContainerEvent {

	private final Id<Person> person;
	private final EpisimContainer container;
	private final EpisimConfigGroup.InfectionParams param;
	private final int leaveTime;
	private final int enterTime;
	private final List<PersonContact> contacts;

	PersonLeavesContainerEventImpl(Id<Person> person, EpisimContainer container, EpisimConfigGroup.InfectionParams param,
								   int leaveTime, int enterTime, List<PersonContact> contacts) {
		this.person = person;
		this.container = container;
		this.param = param;
		this.leaveTime = leaveTime;
		this.enterTime = enterTime;
		this.contacts = contacts;
	}

	@Override
	public Id<Person> getPersonId() {
		return person;
	}

	@Override
	public EpisimContainer getContainer() {
		return container;
	}

	@Override
	public EpisimConfigGroup.InfectionParams getActivity() {
		return param;
	}

	@Nullable
	@Override
	public EpisimConfigGroup.InfectionParams getNextActivity() {
		return null;
	}

	@Nullable
	@Override
	public EpisimConfigGroup.InfectionParams getPrevActivity() {
		return null;
	}

	@Override
	public int getTime() {
		return leaveTime;
	}

	@Override
	public int getEnterTime() {
		return enterTime;
	}

	@Override
	public int getNumberOfContacts() {
		return contacts.size();
	}

	@Override
	public PersonContact getContact(int index) {
		return contacts.get(index);
	}

	@Override
	public Iterator<PersonContact> iterator() {
		return contacts.iterator();
	}
}
