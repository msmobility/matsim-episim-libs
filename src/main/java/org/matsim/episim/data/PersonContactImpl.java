package org.matsim.episim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;

/**
 * Basic implementation of PersonContact with backing fields.
 */
class PersonContactImpl implements PersonContact {

	private final Id<Person> contactPerson;
	private final EpisimConfigGroup.InfectionParams param;
	private final int offset;
	private final int duration;

	PersonContactImpl(Id<Person> contactPersonId, EpisimConfigGroup.InfectionParams param, int offset, int duration) {
		this.contactPerson = contactPersonId;
		this.param = param;
		this.offset = offset;
		this.duration = duration;
	}

	@Override
	public Id<Person> getContactPerson() {
		return contactPerson;
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
