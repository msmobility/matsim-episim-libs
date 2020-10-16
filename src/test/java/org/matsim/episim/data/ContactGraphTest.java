package org.matsim.episim.data;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.EpisimTestUtils;
import org.matsim.facilities.ActivityFacility;

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


	@Test
	public void create() {
		ContactGraph graph = new ContactGraph(generateEvents(), infectionParams);

		assertCorrectGraph(graph);
	}

	@Test
	public void write() throws IOException {

		ContactGraph graph = new ContactGraph(generateEvents(), infectionParams);

		Path tmp = Files.createTempFile("graph", "bin");

		graph.write(FileChannel.open(tmp, StandardOpenOption.WRITE));

		InputStream in = Files.newInputStream(tmp);

		ContactGraph g = new ContactGraph(in, infectionParams);

		assertCorrectGraph(g);
	}

	private void assertCorrectGraph(ContactGraph g) {
		Iterator<PersonLeaveEvent> it = g.iterator();
		PersonLeaveEvent ev = it.next();

		assertThat(ev.getPersonId()).isEqualTo(Id.createPersonId(1).index());
		assertThat(ev.getFacilityId()).isEqualTo(Id.create(2, ActivityFacility.class).index());
		assertThat(ev.isInVehicle()).isEqualTo(true);
		assertThat(ev.getActivity()).isEqualTo(infectionParams.get(0));
		assertThat(ev.getLeaveTime()).isEqualTo(1000);
		assertThat(ev.getEnterTime()).isEqualTo(2000);

		ev = it.next();



		assertThat(it.hasNext()).isFalse();
	}

	private List<PersonLeaveEvent> generateEvents() {
		return List.of(
				PersonLeaveEvent.newInstance(
						Id.createPersonId(1),
						Id.create(2, ActivityFacility.class),
						true,
						infectionParams.get(0),
						1000,
						2000,
						List.of(
								PersonContact.newInstance(
										Id.createPersonId(4),
										infectionParams.get(1),
										100,
										200
								),
								PersonContact.newInstance(
										Id.createPersonId(5),
										infectionParams.get(1),
										300,
										400
								)
						)
				),
				PersonLeaveEvent.newInstance(
						Id.createPersonId(2),
						Id.create(3, ActivityFacility.class),
						false,
						infectionParams.get(1),
						5000,
						7000,
						List.of(
								PersonContact.newInstance(
										Id.createPersonId(6),
										infectionParams.get(1),
										500,
										700
								),
								PersonContact.newInstance(
										Id.createPersonId(7),
										infectionParams.get(1),
										800,
										900
								)
						)
				)
		);
	}


}
