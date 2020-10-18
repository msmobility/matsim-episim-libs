/*-
 * #%L
 * MATSim Episim
 * %%
 * Copyright (C) 2020 matsim-org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.matsim.episim;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.matsim.episim.EpisimUtils.readChars;
import static org.matsim.episim.EpisimUtils.writeChars;

/**
 * Wrapper class for a specific location that keeps track of currently contained agents and entering times.
 */
public class MutableEpisimContainer implements EpisimContainer {
	private final Id<EpisimContainer> containerId;
	private final boolean isVehicle;

	/**
	 * Persons currently in this container. Stored only as Ids.
	 */
	private final IntSet persons = new IntOpenHashSet(4);

	/**
	 * Person list needed to draw random persons within container.
	 */
	private final List<MutableEpisimPerson> personsAsList = new ArrayList<>();

	private final Int2DoubleMap containerEnterTimes = new Int2DoubleOpenHashMap(4);

	/**
	 * The maximum number of persons simultaneously in this container. Negative if unknown.
	 * Already scaled with sampleSize.
	 */
	private int maxGroupSize = -1;

	/**
	 * The number of persons using this container over all days.
	 */
	private int totalUsers = -1;

	/**
	 * Typical number of persons that can fit into this container.
	 */
	private int typicalCapacity = -1;

	/**
	 * Number of distinct spaces in this facility. May be relevant for certain contact models.
	 */
	private double numSpaces = 1;

	/**
	 * Creates a container that is not a vehicle.
	 * @param containerId
	 */
	MutableEpisimContainer(Id<EpisimContainer> containerId) {
		this(containerId, false);
	}

	MutableEpisimContainer(Id<?> containerId, boolean isVehicle) {
		this.containerId = Id.create(containerId.toString(), EpisimContainer.class);
		this.isVehicle = isVehicle;
	}

	/**
	 * Reads containers state from stream.
	 */
	@Override
	public void read(ObjectInput in, Map<Id<Person>, MutableEpisimPerson> persons) throws IOException {

		this.persons.clear();
		this.personsAsList.clear();
		this.containerEnterTimes.clear();

		int n = in.readInt();
		for (int i = 0; i < n; i++) {
			Id<Person> id = Id.create(readChars(in), Person.class);
			this.persons.add(id.index());
			personsAsList.add(persons.get(id));
			containerEnterTimes.put(id.index(), in.readDouble());
		}
	}

	/**
	 * Writes state to stream.
	 */
	@Override
	public void write(ObjectOutput out) throws IOException {

		out.writeInt(containerEnterTimes.size());
		for (MutableEpisimPerson p : personsAsList) {
			writeChars(out, p.getPersonId().toString());
			out.writeDouble(containerEnterTimes.get(p.getPersonId().index()));
		}
	}

	void addPerson(MutableEpisimPerson person, double now) {
		final int index = person.getPersonId().index();

		if (persons.contains(index))
			throw new IllegalStateException("Person already contained in this container.");

		persons.add(index);
		personsAsList.add(person);
		containerEnterTimes.put(index, now);
		person.setCurrentContainer(this);
	}

	/**
	 * Removes a person from this container.
	 *
	 * @throws RuntimeException if the person was not in the container.
	 */
	void removePerson(MutableEpisimPerson person) {
		int index = person.getPersonId().index();

		containerEnterTimes.remove(index);
		persons.remove(index);
		person.removeCurrentContainer(this);
		boolean wasRemoved = personsAsList.remove(person);
		Gbl.assertIf(wasRemoved);
	}

	@Override
	public Id<EpisimContainer> getContainerId() {
		return containerId;
	}

	@Override
	public boolean isVehicle() {
		return isVehicle;
	}

	/**
	 * @return maximum group size in container.
	 */
	public int getMaxGroupSize() {
		return maxGroupSize;
	}

	/**
	 * @return number of people using container.  May be larger than {@link #getMaxGroupSize()}.
	 */
	public int getTotalUsers() {
		return totalUsers;
	}

	public int getTypicalCapacity() {
		return typicalCapacity;
	}

	public double getNumSpaces() {
		return numSpaces;
	}

	/**
	 * Sets the max group size this container has during a day.
	 *
	 * @param num number of persons
	 */
	void setMaxGroupSize(int num) {
		maxGroupSize = num;
	}

	/**
	 * Sets the total number of persons using this container.
	 *
	 * @param num number of persons
	 */
	void setTotalUsers(int num) {
		totalUsers = num;
	}

	void setTypicalCapacity(int typicalCapacity) {
		this.typicalCapacity = typicalCapacity;
	}

	public void setNumSpaces(double numSpaces) {
		this.numSpaces = numSpaces;
	}

	void clearPersons() {
		this.persons.clear();
		this.personsAsList.clear();
		this.containerEnterTimes.clear();
	}

	/**
	 * Returns the time the person entered the container, or {@link Double#NEGATIVE_INFINITY} if it never entered.
	 */
	public double getContainerEnteringTime(Id<Person> personId) {
		return containerEnterTimes.getOrDefault(personId.index(), Double.NEGATIVE_INFINITY);
	}

	public List<MutableEpisimPerson> getPersons() {
		// Using Collections.unmodifiableList(...) puts huge pressure on the GC if its called hundred thousand times per second
		return personsAsList;
	}
}
