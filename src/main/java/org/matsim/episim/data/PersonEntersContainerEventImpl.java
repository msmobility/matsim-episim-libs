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
 * Simple implementation for person enters container event.
 */
public class PersonEntersContainerEventImpl implements PersonEntersContainerEvent {
	private final Id<Person> personId;
	private final EpisimContainer container;
	private final EpisimConfigGroup.InfectionParams param;
	private final int enterTime;

	PersonEntersContainerEventImpl(Id<Person> person, EpisimContainer container, EpisimConfigGroup.InfectionParams param, int enterTime) {
		this.personId = person;
		this.container = container;
		this.param = param;
		this.enterTime = enterTime;
	}

	PersonEntersContainerEventImpl(DataInput in, Int2ObjectMap<Id<Person>> persons, Int2ObjectMap<EpisimContainer> container,
										  EpisimConfigGroup.InfectionParams[] params) throws IOException {
		this.personId = persons.get(in.readInt());
		this.container = container.get(in.readInt());
		this.param = readParam(params, in.readByte());
		this.enterTime = in.readInt();
	}

    @Override
	public EpisimContainer getContainer() {
		return container;
	}

	@Override
	public EpisimConfigGroup.InfectionParams getActivity() {
		return param;
	}

	@Override
	public int getTime() {
		return enterTime;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PersonEntersContainerEventImpl that = (PersonEntersContainerEventImpl) o;
		return enterTime == that.enterTime &&
				personId.equals(that.personId) &&
				container.equals(that.container) &&
				param.equals(that.param);
	}

	@Override
	public int hashCode() {
		return Objects.hash(personId, container, enterTime);
	}
}
