package org.matsim.episim.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.episim.EpisimConfigGroup;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.matsim.episim.data.PersonLeavesContainerEventImpl.writeParam;

/**
 * Interface for episim specific events.
 */
public interface EpisimEvent extends HasPersonId {
	/**
	 * Seconds since start of day when this event happens.
	 */
	int getTime();

	/**
	 * Creates a immutable copy of an event.
	 */
	static EpisimEvent copy(EpisimEvent event) {
		if (event instanceof PersonEntersContainerEvent) {
			return new PersonEntersContainerEventImpl(
					event.getPersonId(), ((PersonEntersContainerEvent) event).getContainer(), ((PersonEntersContainerEvent) event).getActivity(), event.getTime()
			);

		} else if (event instanceof PersonLeavesContainerEvent) {

			List<PersonContact> contacts = StreamSupport.stream(((PersonLeavesContainerEvent) event).spliterator(), false)
					.map(c -> new PersonContactImpl(c.getContactPerson(), c.getContactPersonActivity(), c.getOffset(), c.getDuration()))
					.collect(Collectors.toList());

			return new PersonLeavesContainerEventImpl(
					event.getPersonId(), ((PersonLeavesContainerEvent) event).getContainer(),
					((PersonLeavesContainerEvent) event).getActivity(),
					((PersonLeavesContainerEvent) event).getPrevActivity(),
					((PersonLeavesContainerEvent) event).getNextActivity(),
					event.getTime(), ((PersonLeavesContainerEvent) event).getEnterTime(), contacts
			);
		}

		throw new IllegalArgumentException("Given type of event not known: " + event.getClass());
	}

	/**
	 * Reads event data from binary input.
	 */
	static List<EpisimEvent> read(DataInput in,
							  Int2ObjectMap<Id<Person>> persons, Int2ObjectMap<EpisimContainer> container,
							  EpisimConfigGroup.InfectionParams[] params) {

		try {
			int n = in.readInt();
			List<EpisimEvent> events = new ArrayList<>(n);

			for (int i = 0; i < n; i++) {

				byte b = in.readByte();
				if (b == 0)
					events.add(new PersonEntersContainerEventImpl(in, persons, container, params));
				else if (b == 1)
					events.add(new PersonLeavesContainerEventImpl(in, persons, container, params));
				else
					throw new IllegalStateException("Unknown event type");
			}

			return events;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * write event data to binary input.
	 */
	static void write(DataOutput out, Collection<EpisimEvent> events, List<EpisimConfigGroup.InfectionParams> params) {
		try {
			out.writeInt(events.size());

			for (EpisimEvent e : events) {
				if (e instanceof PersonEntersContainerEvent) {
					out.writeByte(0);
					PersonEntersContainerEvent ev = (PersonEntersContainerEvent) e;

					out.writeInt(ev.getPersonId().index());
					out.writeInt(ev.getContainer().getContainerId().index());
					out.writeByte(writeParam(params, ev.getActivity()));
					out.writeInt(ev.getTime());

				} else if (e instanceof PersonLeavesContainerEvent) {
					out.writeByte(1);
					PersonLeavesContainerEvent ev = (PersonLeavesContainerEvent) e;

					out.writeInt(ev.getPersonId().index());
					out.writeInt(ev.getContainer().getContainerId().index());
					out.writeByte(writeParam(params, ev.getActivity()));
					out.writeByte(writeParam(params, ev.getPrevActivity()));
					out.writeByte(writeParam(params, ev.getNextActivity()));
					out.writeInt(ev.getTime());
					out.writeInt(ev.getEnterTime());
					out.writeInt(ev.getNumberOfContacts());
					for (PersonContact c : ev) {
						out.writeInt(c.getContactPerson().index());
						out.writeByte(writeParam(params, c.getContactPersonActivity()));
						out.writeInt(c.getOffset());
						out.writeInt(c.getDuration());
					}

				} else throw new IllegalStateException("Unknown event type");
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Return the size in bytes for a given collection of events.
	 */
	static long sizeOf(Collection<EpisimEvent> events) {
		// there seems to be always 4 extra byte
		long size = 4;

		for (EpisimEvent e : events) {
			if (e instanceof PersonEntersContainerEvent)
				size += 3 * 4 + 2;
			else if (e instanceof PersonLeavesContainerEvent) {
				size += 5 * 4 + 4 + ((PersonLeavesContainerEvent) e).getNumberOfContacts() * 13;
			}
		}

		return size;
	}

}
