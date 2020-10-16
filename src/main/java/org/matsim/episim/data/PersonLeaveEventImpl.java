package org.matsim.episim.data;

import org.matsim.episim.EpisimConfigGroup;

import java.util.Iterator;
import java.util.List;

/**
 * Basic implementation of the person leave event.
 */
class PersonLeaveEventImpl implements PersonLeaveEvent {

	private final int personId;
	private final int facilityId;
	private final boolean isVehicle;
	private final EpisimConfigGroup.InfectionParams param;
	private final int leaveTime;
	private final int enterTime;
	private final List<PersonContact> contacts;

	PersonLeaveEventImpl(int personId, int facilityId, boolean isVehicle, EpisimConfigGroup.InfectionParams param, int leaveTime, int enterTime, List<PersonContact> contacts) {
		this.personId = personId;
		this.facilityId = facilityId;
		this.isVehicle = isVehicle;
		this.param = param;
		this.leaveTime = leaveTime;
		this.enterTime = enterTime;
		this.contacts = contacts;
	}

	@Override
	public int getPersonId() {
		return personId;
	}

	@Override
	public int getFacilityId() {
		return facilityId;
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
