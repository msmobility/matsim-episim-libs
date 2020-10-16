package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;

import java.util.Iterator;
import java.util.List;

/**
 * Basic implementation of the person leave event.
 */
class PersonLeavesContainerEventImpl implements PersonLeavesContainerEvent {

	private final Id<Person> personId;
	private final Id<EpisimContainer> containerId;
	private final boolean isVehicle;
	private final EpisimConfigGroup.InfectionParams param;
	private final int leaveTime;
	private final int enterTime;
	private final List<PersonContact> contacts;

	PersonLeavesContainerEventImpl(Id<Person> personId, Id<EpisimContainer> containerId, boolean isVehicle,
								   EpisimConfigGroup.InfectionParams param, int leaveTime, int enterTime, List<PersonContact> contacts) {
		this.personId = personId;
		this.containerId = containerId;
		this.isVehicle = isVehicle;
		this.param = param;
		this.leaveTime = leaveTime;
		this.enterTime = enterTime;
		this.contacts = contacts;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}

	@Override
	public Id<EpisimContainer> getContainerId() {
		return containerId;
	}

	@Override
	public boolean isInVehicle() {
		return isVehicle;
	}

	@Override
	public EpisimConfigGroup.InfectionParams getActivity() {
		return param;
	}

	@Override
	public int getLeaveTime() {
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
	public Iterator<PersonContact> iterator() {
		return contacts.iterator();
	}
}
