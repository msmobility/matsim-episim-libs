package org.matsim.episim.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;

import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
	private final PersonContact[] contacts;

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
		this.contacts = contacts.toArray(new PersonContact[0]);
	}

	PersonLeavesContainerEventImpl(DataInput in,
								   Int2ObjectMap<Id<Person>> persons, Int2ObjectMap<EpisimContainer> container,
								   EpisimConfigGroup.InfectionParams[] params) throws IOException {

		this.person = persons.get(in.readInt());
		this.container = container.get(in.readInt());
		this.activity = readParam(params, in.readByte());
		this.prevActivity = readParam(params, in.readByte());
		this.nextActivity = readParam(params, in.readByte());
		this.leaveTime = in.readInt();
		this.enterTime = in.readInt();
		this.contacts = new PersonContact[in.readInt()];
		for (int i = 0; i < this.contacts.length; i++) {
			this.contacts[i] = PersonContact.read(in, persons, params);
		}
	}

	static EpisimConfigGroup.InfectionParams readParam(EpisimConfigGroup.InfectionParams[] params, byte idx) {
		return idx > -1 ? params[idx] : null;
	}

	static byte writeParam(List<EpisimConfigGroup.InfectionParams> params, EpisimConfigGroup.InfectionParams param) {
		return (byte) (param == null ? -1 : params.indexOf(param));
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
		return contacts.length;
	}

	@Override
	public PersonContact getContact(int index) {
		return contacts[index];
	}

	@Override
	public Iterator<PersonContact> iterator() {
		return new ContactItr();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PersonLeavesContainerEventImpl that = (PersonLeavesContainerEventImpl) o;
		return leaveTime == that.leaveTime &&
				enterTime == that.enterTime &&
				person.equals(that.person) &&
				container.equals(that.container) &&
				activity.equals(that.activity) &&
				Objects.equals(prevActivity, that.prevActivity) &&
				Objects.equals(nextActivity, that.nextActivity) &&
				Arrays.equals(contacts, that.contacts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, container, leaveTime, enterTime);
	}

	/**
	 * Iterate over contact array.
	 */
	private final class ContactItr implements Iterator<PersonContact> {

		private int index = 0;
		private final int length = contacts.length;

		@Override
		public boolean hasNext() {
			return index < length;
		}

		@Override
		public PersonContact next() {
			return contacts[index++];
		}
	}
}
