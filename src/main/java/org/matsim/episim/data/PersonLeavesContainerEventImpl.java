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
	private final EpisimConfigGroup.InfectionParams activity;
	private final EpisimConfigGroup.InfectionParams prevActivity;
	private final EpisimConfigGroup.InfectionParams nextActivity;
	private final int leaveTime;
	private final int enterTime;
	private final List<PersonContact> contacts;

	PersonLeavesContainerEventImpl(Id<Person> person, EpisimContainer container, EpisimConfigGroup.InfectionParams activity,
								   EpisimConfigGroup.InfectionParams prevActivity, EpisimConfigGroup.InfectionParams nextActivity,
								   int leaveTime, int enterTime, List<PersonContact> contacts) {
		this.person = person;
		this.container = container;
		this.activity = activity;
		this.prevActivity = prevActivity;
		this.nextActivity = nextActivity;
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
		return activity;
	}

	@Nullable
	@Override
	public EpisimConfigGroup.InfectionParams getNextActivity() {
		return nextActivity;
	}

	@Nullable
	@Override
	public EpisimConfigGroup.InfectionParams getPrevActivity() {
		return prevActivity;
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
