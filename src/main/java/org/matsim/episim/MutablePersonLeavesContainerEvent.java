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

	/**
	 * Instance to retrieve person contacts.
	 */
	private final MutablePersonContact contact = new MutablePersonContact();

	/**
	 * Reusable iterator.
	 */
	private final ContactIterator it = new ContactIterator();

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
		it.reset();
		return it;
	}

	@Override
	public PersonContact getContact(int index) {
		if (container.getPersons().get(index) == person)
			index += 1;

		if (index >= container.getPersons().size())
			throw new IllegalArgumentException("Index " + index + " out of bounds for contacts of size " + container.getPersons().size());

		contact.setPerson(container.getPersons().get(index));

		return contact;
	}

	@Override
	public Id<Person> getPersonId() {
		return person.getPersonId();
	}


	private final class MutablePersonContact implements PersonContact {

		/**
		 * Contact person.
		 */
		private MutableEpisimPerson contactPerson;

		private void setPerson(MutableEpisimPerson person) {
			contactPerson = person;
		}

		@Override
		public Id<Person> getContactPerson() {
			return person.getPersonId();
		}

		@Override
		public EpisimConfigGroup.InfectionParams getContactPersonActivity() {
			return contactPerson.getTrajectory().get(contactPerson.getCurrentPositionInTrajectory()).params;
		}

		@Override
		public int getOffset() {
			return (int) (container.getContainerEnteringTime(contactPerson.getPersonId()) - getEnterTime());
		}

		@Override
		public int getDuration() {
			return (int) (now - Math.max(container.getContainerEnteringTime(person.getPersonId()), container.getContainerEnteringTime(contactPerson.getPersonId())));
		}
	}

	/**
	 * Iterates over the contacts of a person in a container.
	 */
	private final class ContactIterator implements Iterator<PersonContact> {

		private int index = -1;

		private void reset() {
			index = -1;
		}

		@Override
		public boolean hasNext() {
			return index < getNumberOfContacts();
		}

		@Override
		public PersonContact next() {
			index++;
			return getContact(index);
		}
	}

}
