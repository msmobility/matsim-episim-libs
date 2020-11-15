package org.matsim.episim.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.EpisimTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ContactGraphTest {

	private Config config = EpisimTestUtils.createTestConfig();
	private List<EpisimConfigGroup.InfectionParams> infectionParams =
			new ArrayList<>(ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class).getInfectionParams());

	private static final Int2ObjectMap<EpisimContainer> container = new Int2ObjectArrayMap<>();
	private static final Int2ObjectMap<Id<Person>> persons = new Int2ObjectArrayMap<>();

	static {
		for (int i = 0; i < 5; i++) {
			EpisimPerson p = EpisimTestUtils.createPerson(null);
			persons.put(p.getPersonId().index(), p.getPersonId());
			EpisimContainer c = EpisimTestUtils.createFacility();
			container.put(c.getContainerId().index(), c);
		}
	}


	@Test
	public void create() {
		ContactGraph graph = new ContactGraph(generateEvents(), infectionParams, persons, container);

		assertCorrectGraph(graph);
	}


	@Test
	public void write() throws IOException {

		ContactGraph graph = new ContactGraph(generateEvents(), infectionParams, persons, container);

		Path tmp = Files.createTempFile("graph", "bin");

		graph.write(FileChannel.open(tmp, StandardOpenOption.WRITE));

		InputStream in = Files.newInputStream(tmp);

		ContactGraph g = new ContactGraph(in, infectionParams, persons, container);

		assertCorrectGraph(g);
	}

	private void assertCorrectGraph(ContactGraph g) {
		Iterator<EpisimEvent> it = g.iterator();
		PersonLeavesContainerEvent ev = (PersonLeavesContainerEvent) it.next();

		assertThat(ev.getPersonId()).isEqualTo(persons.get(0));
		assertThat(ev.getContainer()).isEqualTo(container.get(1));
		assertThat(ev.getActivity()).isEqualTo(infectionParams.get(0));
		assertThat(ev.getTime()).isEqualTo(1000);
		assertThat(ev.getEnterTime()).isEqualTo(2000);

		assertThat(ev)
				.hasSize(2);

		assertThat(it.hasNext()).isTrue();

		ev = (PersonLeavesContainerEvent) it.next();

		assertThat(ev.getPersonId()).isEqualTo(persons.get(1));
		assertThat(ev.getContainer()).isEqualTo(container.get(2));
		assertThat(ev.getTime()).isEqualTo(5000);

		assertThat(ev)
				.allMatch(p -> p.getContactPerson().equals(persons.get(4)) &&
						p.getContactPersonActivity() == infectionParams.get(1) &&
						p.getOffset() == 500 && p.getDuration() == 700)
				.hasSize(1);

		assertThat(ev.getContact(0).getOffset()).isEqualTo(500);
		assertThat(ev.getContact(0).getDuration()).isEqualTo(700);

		assertThat(it.hasNext()).isTrue();

		PersonEntersContainerEvent en = (PersonEntersContainerEvent) it.next();

		assertThat(en.getPersonId()).isEqualTo(persons.get(3));
		assertThat(en.getContainer()).isEqualTo(container.get(3));
		assertThat(en.getActivity()).isEqualTo(infectionParams.get(2));
		assertThat(en.getTime()).isEqualTo(3000);

		assertThat(it.hasNext()).isFalse();
	}

	private List<EpisimEvent> generateEvents() {
		return List.of(
				PersonLeavesContainerEvent.newInstance(
						persons.get(0),
						container.get(1),
						infectionParams.get(0),
						1000,
						2000,
						List.of(
								PersonContact.newInstance(
										persons.get(3),
										infectionParams.get(1),
										100,
										200
								),
								PersonContact.newInstance(
										persons.get(2),
										infectionParams.get(1),
										300,
										400
								)
						)
				),
				PersonLeavesContainerEvent.newInstance(
						persons.get(1),
						container.get(2),
						infectionParams.get(1),
						5000,
						7000,
						List.of(
								PersonContact.newInstance(
										persons.get(4),
										infectionParams.get(1),
										500,
										700
								)
						)
				),
				PersonEntersContainerEvent.newInstance(
						persons.get(3),
						container.get(3),
						infectionParams.get(2),
						3000
				)
		);
	}


}
