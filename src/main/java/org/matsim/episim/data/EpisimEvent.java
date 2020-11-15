package org.matsim.episim.data;

import org.matsim.core.api.internal.HasPersonId;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

}
