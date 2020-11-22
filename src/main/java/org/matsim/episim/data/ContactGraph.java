package org.matsim.episim.data;


import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.Iterator;

/**
 * Completely static and efficient representation of a contact graph.
 * Stored similar as a CSR graph and off-heap.
 * <p>
 * When the graph is not needed anymore, {@link #close()} needs to be called to free the memory again.s
 */
public class ContactGraph implements Iterable<EpisimEvent>, Closeable {

	private static final Logger log = LogManager.getLogger(ContactGraph.class);

	/**
	 * Access to "unsafe" features.
	 */
	private static final Unsafe UNSAFE;

	/**
	 * Fields required to create {@link ByteBuffer}.
	 */
	private static final Field address, capacity;

	static {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			UNSAFE = (Unsafe) f.get(null);
			address = Buffer.class.getDeclaredField("address");
			address.setAccessible(true);
			capacity = Buffer.class.getDeclaredField("capacity");
			capacity.setAccessible(true);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException("Could not retrieve unsafe instance", e);
		}
	}

	/**
	 * Create a bytebuffer wrapping an address.
	 */
	private static ByteBuffer wrapAddress(long addr, int length) {
		ByteBuffer bb = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());
		try {
			address.setLong(bb, addr);
			capacity.setInt(bb, length);
			bb.clear();
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		}
		return bb;
	}

	/**
	 * Map of person ids.
	 */
	private final Int2ObjectMap<Id<Person>> persons;

	/**
	 * Map of available container.
	 */
	private final Int2ObjectMap<EpisimContainer> container;

	/**
	 * Assigns each activity type a certain byte as id.
	 */
	private final Object2ByteMap<EpisimConfigGroup.InfectionParams> activityMap;

	/**
	 * Reverse of {@link #activityMap}.
	 */
	private final EpisimConfigGroup.InfectionParams[] activityTypes;

	/**
	 * Number of bytes per event entry.
	 */
	private static final int EVENT_BYTES = 26;

	/**
	 * Array of events when person leaves a "container". Order by leave time.
	 * <p>
	 * One entry has the following length and data format:
	 * <p>
	 * 8bit 32bit 32bit 8bit 32bit 32bit 8bit 8bit 32bit 16bit = 176bit
	 * <p>
	 * eventType personId containerId activity time enterTime nextActivity prevActivity contactIndex nContacts
	 * <p>
	 */
	private final long events;

	/**
	 * Number of events.
	 */
	private final int numEvents;

	/**
	 * Number of bytes per contact entry.
	 */
	private static final int CONTACT_BYTES = 9;

	/**
	 * Array of all contacts in a facility, pointed to by the entries of {@link #events}.
	 * <p>s
	 * One contact entry has the following length and data format:
	 * <p>
	 * 32bit 8bit 16bit 16bit = 72bit
	 * <p>
	 * contactPersonId contactPersonActivity offset(short) duration(short)
	 */
	private final long contacts;

	/**
	 * Number of contacts.
	 */
	private final int numContacts;

	/**
	 * Create graph by reading it from dist.
	 */
	public ContactGraph(InputStream in, Collection<EpisimConfigGroup.InfectionParams> infectionParams,
				 Int2ObjectMap<Id<Person>> persons, Int2ObjectMap<EpisimContainer> container) throws IOException {

		ReadableByteChannel c = Channels.newChannel(in);
		ByteBuffer sizes = ByteBuffer.allocate(Integer.BYTES * 2);

		IOUtils.read(c, sizes);
		sizes.rewind();

		numEvents = sizes.getInt();
		numContacts = sizes.getInt();
		sizes.clear();

		// allocate and transfer memory
		events = UNSAFE.allocateMemory(numEvents * EVENT_BYTES);
		contacts = UNSAFE.allocateMemory(numContacts * CONTACT_BYTES);

		IOUtils.read(c, wrapAddress(events, numEvents * EVENT_BYTES));
		IOUtils.read(c, wrapAddress(contacts, numContacts * CONTACT_BYTES));

		activityTypes = infectionParams.toArray(new EpisimConfigGroup.InfectionParams[0]);
		activityMap = new Object2ByteArrayMap<>();
		for (int i = 0; i < activityTypes.length; i++) {
			activityMap.put(activityTypes[i], (byte) i);
		}

		this.persons = persons;
		this.container = container;
	}


	/**
	 * Creates static graph from list of events.
	 */
	public ContactGraph(Collection<EpisimEvent> eventList, Collection<EpisimConfigGroup.InfectionParams> infectionParams,
				 Int2ObjectMap<Id<Person>> persons, Int2ObjectMap<EpisimContainer> container) {

		numEvents = eventList.size();

		numContacts = eventList.stream()
				.filter(e -> e instanceof PersonLeavesContainerEvent)
				.map(e -> (PersonLeavesContainerEvent) e)
				.mapToInt(PersonLeavesContainerEvent::getNumberOfContacts).sum();

		events = UNSAFE.allocateMemory(numEvents * EVENT_BYTES);
		contacts = UNSAFE.allocateMemory(numContacts * CONTACT_BYTES);

		activityTypes = infectionParams.toArray(new EpisimConfigGroup.InfectionParams[0]);
		activityMap = new Object2ByteArrayMap<>();
		for (int i = 0; i < activityTypes.length; i++) {
			activityMap.put(activityTypes[i], (byte) i);
		}

		this.persons = persons;
		this.container = container;

		copyData(eventList);
	}

	/**
	 * Copy data from objects into the memory.
	 */
	private void copyData(Collection<EpisimEvent> eventList) {

		long addr = events;
		int contactIdx = 0;

		LeavesContainerEvent lv = new LeavesContainerEvent(null);
		EntersContainerEvent en = new EntersContainerEvent();


		Contact c = new Contact(contactIdx);
		for (EpisimEvent event : eventList) {

			if (event instanceof PersonLeavesContainerEvent) {

				PersonLeavesContainerEvent e = (PersonLeavesContainerEvent) event;

				UNSAFE.putByte(addr, (byte) 1);

				lv.setAddr(addr);
				lv.setContactIndex(contactIdx);
				lv.setContainerId(e.getContainer().getContainerId().index());
				lv.setPersonId(e.getPersonId().index());
				lv.setActivity(e.getActivity());
				lv.setNextActivity(e.getNextActivity());
				lv.setPrevActivity(e.getPrevActivity());
				lv.setEnterTime(e.getEnterTime());
				lv.setTime(e.getTime());
				lv.setNumberOfContacts(e.getNumberOfContacts());

				for (PersonContact pc : e) {
					c.setIndex(contactIdx);
					c.setContactPersonId(pc.getContactPerson().index());
					c.setOffset(pc.getOffset());
					c.setDuration(pc.getDuration());
					c.setContactPersonActivity(pc.getContactPersonActivity());

					contactIdx++;
				}

			} else if (event instanceof PersonEntersContainerEvent) {

				PersonEntersContainerEvent e = (PersonEntersContainerEvent) event;

				UNSAFE.putByte(addr, (byte) 0);

				en.setAddr(addr);
				en.setActivity(e.getActivity());
				en.setContainerId(e.getContainer().getContainerId().index());
				en.setTime(e.getTime());
				en.setPersonId(e.getPersonId().index());

			}

			addr += EVENT_BYTES;
		}
	}

	/**
	 * Write this graph to disk.
	 */
	public void write(WritableByteChannel c) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES * 2);
		buf.putInt(numEvents);
		buf.putInt(numContacts);
		buf.rewind();

		log.debug("Writing {}MB", getSize() / (1024 * 1024));

		c.write(buf);
		c.write(wrapAddress(events, numEvents * EVENT_BYTES));
		c.write(wrapAddress(contacts, numContacts * CONTACT_BYTES));
	}

	/**
	 * Return size in bytes
	 */
	public long getSize() {
		return Integer.BYTES * 2 + numEvents * EVENT_BYTES + numContacts * CONTACT_BYTES;
	}

	@Override
	public void close() throws IOException {
		UNSAFE.freeMemory(contacts);
		UNSAFE.freeMemory(events);
	}


	@Override
	public Iterator<EpisimEvent> iterator() {
		return new EventIterator();
	}

	private abstract class InMemoryEvent {

		/**
		 * Pointing to entry in {@link #events}.
		 */
		protected long addr;

		public void setAddr(long addr) {
			this.addr = addr;
		}

		public int getTime() {
			return UNSAFE.getInt(addr + 10);
		}

		protected void setTime(int time) {
			UNSAFE.putInt(addr + 10, time);
		}

		public Id<Person> getPersonId() {
			return persons.get(UNSAFE.getInt(addr + 1));
		}

		protected void setPersonId(int personId) {
			UNSAFE.putInt(addr + 1, personId);
		}

		public EpisimContainer getContainer() {
			return container.get(UNSAFE.getInt(addr + 5));
		}

		protected void setContainerId(int facilityId) {
			UNSAFE.putInt(addr + 5, facilityId);
		}

		public EpisimConfigGroup.InfectionParams getActivity() {
			return activityTypes[UNSAFE.getByte(addr + 9)];
		}

		protected void setActivity(EpisimConfigGroup.InfectionParams param) {
			UNSAFE.putByte(addr + 9, activityMap.getByte(param));
		}

	}

	/**
	 * Enter container event.
	 */
	private final class EntersContainerEvent extends InMemoryEvent implements PersonEntersContainerEvent {
	}

	/**
	 * Person leaving a specific facility at certain time. Points to {@link #events}.
	 */
	private final class LeavesContainerEvent extends InMemoryEvent implements PersonLeavesContainerEvent {

		private final ContactIterator it;

		private LeavesContainerEvent(ContactIterator it) {
			this.it = it;
		}

		@Nullable
		@Override
		public EpisimConfigGroup.InfectionParams getNextActivity() {
			byte value = UNSAFE.getByte(addr + 18);
			return value == -1 ? null : activityTypes[value];
		}

		protected void setNextActivity(EpisimConfigGroup.InfectionParams param) {
			byte value = param == null ? -1 : activityMap.getByte(param);
			UNSAFE.putByte(addr + 14, value);
		}

		@Nullable
		@Override
		public EpisimConfigGroup.InfectionParams getPrevActivity() {
			byte value = UNSAFE.getByte(addr + 19);
			return value == -1 ? null : activityTypes[value];
		}

		protected void setPrevActivity(EpisimConfigGroup.InfectionParams param) {
			byte value = param == null ? -1 : activityMap.getByte(param);
			UNSAFE.putByte(addr + 15, value);
		}

		public int getEnterTime() {
			return UNSAFE.getInt(addr + 14);
		}

		private void setEnterTime(int time) {
			UNSAFE.putInt(addr + 14, time);
		}

		/**
		 * Index to {@link #contacts}.
		 */
		private int getContactIndex() {
			return UNSAFE.getInt(addr + 20);
		}

		private void setContactIndex(int index) {
			UNSAFE.putInt(addr + 20, index);
		}

		public int getNumberOfContacts() {
			return Short.toUnsignedInt(UNSAFE.getShort(addr + 24));
		}

		private void setNumberOfContacts(int n) {
			UNSAFE.putShort(addr + 24, (short) n);
		}

		@Override
		public PersonContact getContact(int index) {
			it.contact.setIndex(getContactIndex() + index);
			return it.contact;
		}

		@Override
		public Iterator<PersonContact> iterator() {
			it.reset(getContactIndex(), getNumberOfContacts());
			return it;
		}
	}

	/**
	 * Iterator for iterating over all contacts in a facility.
	 */
	private final class ContactIterator implements Iterator<PersonContact> {

		private final Contact contact;
		private int end;
		private int current;

		public ContactIterator(int containerIndex, int n) {
			current = containerIndex;
			end = containerIndex + n;

			// start at lower index, because it will be increased first
			contact = new Contact(current - 1);
		}

		/**
		 * Reset position of the contact iterator.
		 */
		private void reset(int containerIndex, int n) {
			current = containerIndex;
			end = containerIndex + n;
			contact.setIndex(containerIndex - 1);
		}

		@Override
		public boolean hasNext() {
			return current < end;
		}

		@Override
		public Contact next() {
			contact.next();
			current++;
			return contact;
		}
	}

	/**
	 * Accessor to contact information from the graph. Don't store this class.
	 * Points to {@link #contacts}
	 */
	public final class Contact implements PersonContact {

		/**
		 * Pointing to entry in {@link #contacts}.
		 */
		private long addr;

		public Contact(int contactIndex) {
			this.addr = contacts + contactIndex * CONTACT_BYTES;
		}

		private void setIndex(int contactIndex) {
			this.addr = contacts + contactIndex * CONTACT_BYTES;
		}

		/**
		 * Move pointer to next entry.
		 */
		private void next() {
			this.addr += CONTACT_BYTES;
		}

		public Id<Person> getContactPerson() {
			return persons.get(UNSAFE.getInt(addr));
		}

		private void setContactPersonId(int contactPersonId) {
			UNSAFE.putInt(addr, contactPersonId);
		}

		public int getOffset() {
			return Short.toUnsignedInt(UNSAFE.getShort(addr + 4)) >> 2;
		}

		private void setOffset(int offset) {
			UNSAFE.putShort(addr + 4, (short) (offset << 2));
		}

		public int getDuration() {
			return Short.toUnsignedInt(UNSAFE.getShort(addr + 6)) >> 2;
		}

		private void setDuration(int offset) {
			UNSAFE.putShort(addr + 6, (short) (offset << 2));
		}

		public EpisimConfigGroup.InfectionParams getContactPersonActivity() {
			return activityTypes[UNSAFE.getByte(addr + 8)];
		}

		private void setContactPersonActivity(EpisimConfigGroup.InfectionParams param) {
			UNSAFE.putByte(addr + 8, activityMap.getByte(param));
		}

	}

	/**
	 * Iterates over all {@link LeavesContainerEvent} in the graph.
	 */
	private final class EventIterator implements Iterator<EpisimEvent> {

		/**
		 * Contact iterator re-used when iterating over the individual events
		 */
		private final ContactIterator contacts = new ContactIterator(0, 0);
		/**
		 * The events that are re-used and giving access to the data.
		 */
		private final LeavesContainerEvent event = new LeavesContainerEvent(contacts);
		private final EntersContainerEvent enterEvent = new EntersContainerEvent();

		private int index;
		private long addr = events;

		public EventIterator() {
			index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < numEvents;
		}

		@Override
		public EpisimEvent next() {
			event.setAddr(addr);

			index++;

			byte type = UNSAFE.getByte(addr);
			if (type == 0) {
				enterEvent.setAddr(addr);
				addr += EVENT_BYTES;
				return enterEvent;
			} else if (type == 1) {
				event.setAddr(addr);
				addr += EVENT_BYTES;
				return event;
			}

			return event;
		}
	}
}
