package org.matsim.episim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

import java.util.Map;

/**
 * Notifies about performed vaccinations.
 */
public class EpisimVaccinationEvent extends Event implements HasPersonId {

	public static final String EVENT_TYPE = "episimVaccination";
	public static final String RE_VACCINATION = "reVaccination";

	private final Id<Person> personId;
	private final boolean reVaccination;

	public EpisimVaccinationEvent(double time, Id<Person> personId, boolean reVaccination) {
		super(time);
		this.personId = personId;
		this.reVaccination = reVaccination;
	}


	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(RE_VACCINATION, String.valueOf(reVaccination));

		return attr;
	}
}
