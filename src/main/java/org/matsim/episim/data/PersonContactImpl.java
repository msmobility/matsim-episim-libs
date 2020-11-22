package org.matsim.episim.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;

import java.io.DataInput;
import java.io.IOException;
import java.util.Objects;

import static org.matsim.episim.data.PersonLeavesContainerEventImpl.readParam;

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

	PersonContactImpl(DataInput in, Int2ObjectMap<Id<Person>> persons, EpisimConfigGroup.InfectionParams[] params) throws IOException {
		this.contactPerson = persons.get(in.readInt());
		this.param = readParam(params, in.readByte());
		this.offset = in.readInt();
		this.duration = in.readInt();
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PersonContactImpl that = (PersonContactImpl) o;
		return offset == that.offset &&
				duration == that.duration &&
				contactPerson.equals(that.contactPerson) &&
				param.equals(that.param);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contactPerson, offset, duration);
	}
}
