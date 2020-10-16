package org.matsim.episim.data;

import org.matsim.episim.EpisimConfigGroup;

/**
 * Basic implementation of PersonContact with backing fields.
 */
class PersonContactImpl implements PersonContact {

	private final int contactPersonId;
	private final EpisimConfigGroup.InfectionParams param;
	private final int offset;
	private final int duration;

	PersonContactImpl(int contactPersonId, EpisimConfigGroup.InfectionParams param, int offset, int duration) {
		this.contactPersonId = contactPersonId;
		this.param = param;
		this.offset = offset;
		this.duration = duration;
	}

	@Override
	public int getContactPersonId() {
		return contactPersonId;
	}

	@Override
	public EpisimConfigGroup.InfectionParams getContactPersonActivity() {
		return param;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
	public int getDuration() {
		return duration;
	}
}
