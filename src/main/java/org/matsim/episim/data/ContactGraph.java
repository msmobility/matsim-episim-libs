package org.matsim.episim.data;


import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import org.apache.commons.io.IOUtils;
import org.matsim.episim.EpisimConfigGroup;
import sun.misc.Unsafe;

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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Completely static and efficient representation of a contact graph.
 * Stored similar as a CSR graph and off-heap.
 * <p>
 * When the graph is not needed anymore, {@link #close()} needs to be called to free the memory again.s
 */
public class ContactGraph implements Iterable<PersonLeaveEvent>, Closeable {

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
	private static final int EVENT_BYTES = 20;

	/**
	 * Array of events when person leaves a "container". Order by leave time.
	 * <p>
	 * One entry has the following length and data format:
	 * <p>
	 * 32bit 32bit 8bit 8bit 16bit 16bit 32bit 16bit = 160bit
	 * <p>
	 * personId facilityId isVehicle activity leaveTime enterTime contactIndex nContacts
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
	ContactGraph(InputStream in, Collection<EpisimConfigGroup.InfectionParams> infectionParams) throws IOException {

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
	}


	/**
	 * Creates static graph from list of events.
	 */
	ContactGraph(List<PersonLeaveEvent> eventList, Collection<EpisimConfigGroup.InfectionParams> infectionParams) {

		numEvents = eventList.size();
		numContacts = eventList.stream().mapToInt(PersonLeaveEvent::getNumberOfContacts).sum();

		events = UNSAFE.allocateMemory(numEvents * EVENT_BYTES);
		contacts = UNSAFE.allocateMemory(numContacts * CONTACT_BYTES);

		activityTypes = infectionParams.toArray(new EpisimConfigGroup.InfectionParams[0]);
		activityMap = new Object2ByteArrayMap<>();
		for (int i = 0; i < activityTypes.length; i++) {
			activityMap.put(activityTypes[i], (byte) i);
		}

		copyData(eventList);
	}

	/**
	 * Copy data from objects into the memory.
	 */
	private void copyData(List<PersonLeaveEvent> eventList) {

		int i = 0;
		int contactIdx = 0;

		LeaveEvent it = new LeaveEvent(null);
		Contact c = new Contact(contactIdx);
		for (PersonLeaveEvent event : eventList) {
			it.setIndex(i);
			it.setContactIndex(contactIdx);
			it.setFacilityId(event.getFacilityId());
			it.setPersonId(event.getPersonId());
			it.setActivity(event.getActivity());
			it.setEnterTime(event.getEnterTime());
			it.setLeaveTime(event.getLeaveTime());
			it.setNumberOfContacts(event.getNumberOfContacts());
			it.setInVehicle(event.isInVehicle());

			for (PersonContact pc : event) {
				c.setIndex(contactIdx);
				c.setContactPersonId(pc.getContactPersonId());
				c.setOffset(pc.getOffset());
				c.setDuration(pc.getDuration());
				c.setContactPersonActivity(pc.getContactPersonActivity());

				contactIdx++;
			}

			i++;
		}
	}

	/**
	 * Write this graph to disk.
	 */
	public void write(FileChannel fc) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES * 2);
		buf.putInt(numEvents);
		buf.putInt(numContacts);
		buf.rewind();

		fc.write(buf);
		fc.write(wrapAddress(events, numEvents * EVENT_BYTES));
		fc.write(wrapAddress(contacts, numContacts * CONTACT_BYTES));
	}

	@Override
	public void close() throws IOException {
		UNSAFE.freeMemory(contacts);
		UNSAFE.freeMemory(events);
	}


	@Override
	public Iterator<PersonLeaveEvent> iterator() {
		return new EventIterator();
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

		public int getContactPersonId() {
			return UNSAFE.getInt(addr);
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
	 * Person leaving a specific facility at certain time. Points to {@link #events}.
	 */
	public final class LeaveEvent implements PersonLeaveEvent {

		private final ContactIterator it;

		/**
		 * Pointing to entry in {@link #events}.
		 */
		private long addr;

		private LeaveEvent(ContactIterator it) {
			this.it = it;
		}

		public int getPersonId() {
			return UNSAFE.getInt(addr);
		}

		private void setPersonId(int personId) {
			UNSAFE.putInt(addr, personId);
		}

		public int getFacilityId() {
			return UNSAFE.getInt(addr + 4);
		}

		private void setFacilityId(int facilityId) {
			UNSAFE.putInt(addr + 4, facilityId);
		}

		public boolean isInVehicle() {
			return UNSAFE.getByte(addr + 8) != 0;
		}

		private void setInVehicle(boolean b) {
			UNSAFE.putByte(addr + 8, (byte) (b ? 255 : 0));
		}

		public EpisimConfigGroup.InfectionParams getActivity() {
			return activityTypes[UNSAFE.getByte(addr + 9)];
		}

		private void setActivity(EpisimConfigGroup.InfectionParams param) {
			UNSAFE.putByte(addr + 9, activityMap.getByte(param));
		}

		public int getLeaveTime() {
			return Short.toUnsignedInt(UNSAFE.getShort(addr + 10)) >> 2;
		}

		private void setLeaveTime(int time) {
			UNSAFE.putShort(addr + 10, (short) (time << 2));
		}

		public int getEnterTime() {
			return Short.toUnsignedInt(UNSAFE.getShort(addr + 12)) >> 2;
		}

		private void setEnterTime(int time) {
			UNSAFE.putShort(addr + 12, (short) (time << 2));
		}

		/**
		 * Index to {@link #contacts}.
		 */
		private int getContactIndex() {
			return UNSAFE.getInt(addr + 14);
		}

		private void setContactIndex(int index) {
			UNSAFE.putInt(addr + 14, index);
		}

		public int getNumberOfContacts() {
			return Short.toUnsignedInt(UNSAFE.getShort(addr + 18));
		}

		private void setNumberOfContacts(int n) {
			UNSAFE.putShort(addr + 18, (short) n);
		}

		@Override
		public Iterator<PersonContact> iterator() {
			it.reset(getContactIndex(), getNumberOfContacts());
			return it;
		}

		private void setIndex(int index) {
			addr = events + (index * EVENT_BYTES);
		}

		private void next() {
			addr += EVENT_BYTES;
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
	 * Iterates over all {@link LeaveEvent} in the graph.
	 */
	private final class EventIterator implements Iterator<PersonLeaveEvent> {

		/**
		 * Contact iterator re-used when iterating over the individual events
		 */
		private final ContactIterator contacts = new ContactIterator(0, 0);
		/**
		 * The one event that is re-used and giving access to the data.
		 */
		private final LeaveEvent event = new LeaveEvent(contacts);

		private int index;

		public EventIterator() {
			index = 0;
			event.setIndex(-1);
		}

		@Override
		public boolean hasNext() {
			return index < numEvents;
		}

		@Override
		public LeaveEvent next() {
			event.next();
			index++;
			return event;
		}
	}
}
