package org.matsim.episim;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.data.DiseaseStatus;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MutableEpisimPersonTest {

	@Test
	public void daysSince() {

		MutableEpisimPerson p = EpisimTestUtils.createPerson("work", null);
		double now = EpisimUtils.getCorrectedTime(0, 0, 5);

		p.setDiseaseStatus(now, DiseaseStatus.infectedButNotContagious);
		assertThat(p.daysSince(DiseaseStatus.infectedButNotContagious, 5))
				.isEqualTo(0);

		assertThat(p.daysSince(DiseaseStatus.infectedButNotContagious, 10))
				.isEqualTo(5);

		// change during the third day
		now = EpisimUtils.getCorrectedTime(0, 3600, 3);
		p.setDiseaseStatus(now, DiseaseStatus.critical);
		assertThat(p.daysSince(DiseaseStatus.critical, 4))
				.isEqualTo(1);

		now = EpisimUtils.getCorrectedTime(0, 24 * 60 * 60 - 1, 4);
		p.setDiseaseStatus(now, DiseaseStatus.recovered);
		assertThat(p.daysSince(DiseaseStatus.recovered, 4))
				.isEqualTo(0);

	}


	@Test
	public void isTraceable() {

		MutableEpisimPerson p1 = EpisimTestUtils.createPerson("work", null);
		MutableEpisimPerson p2 = EpisimTestUtils.createPerson("work", null);

		p1.addTraceableContactPerson(p2, 0);
		assertThat(p1.getTraceableContactPersons(0)).containsExactly(p2);

		p1.clearTraceableContractPersons(Integer.MAX_VALUE);

		p1.setTraceable(true);
		p2.setTraceable(false);

		assertThat(p1.isTraceable()).isTrue();

		// not traced because p2 is not traceable
		p1.addTraceableContactPerson(p2, 0);
		assertThat(p1.getTraceableContactPersons(0))
				.isEmpty();

		p2.setTraceable(true);

		p1.addTraceableContactPerson(p2, 0);
		assertThat(p1.getTraceableContactPersons(0)).containsExactly(p2);

	}

	@Test
	public void readWrite() throws IOException {

		MutableEpisimPerson p1 = EpisimTestUtils.createPerson("work", null);

		p1.addTraceableContactPerson(EpisimTestUtils.createPerson("home", null), 100);
		p1.setDiseaseStatus(100, DiseaseStatus.showingSymptoms);
		p1.setTraceable(true);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream bout = new ObjectOutputStream(out);

		p1.write(bout);

		bout.flush();

		Map<Id<Person>, MutableEpisimPerson> persons = new HashMap<>();

		MutableEpisimPerson p2 = EpisimTestUtils.createPerson("c1.0", null);
		p2.read(new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())), persons,  null);

		assertThat(p2.getDiseaseStatus())
				.isEqualTo(DiseaseStatus.showingSymptoms);

	}
}
