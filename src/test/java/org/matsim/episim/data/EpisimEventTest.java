package org.matsim.episim.data;

import org.junit.Test;
import org.matsim.episim.EpisimConfigGroup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class EpisimEventTest {

	@Test
	public void read_write() throws IOException {
		ContactGraphTest t = new ContactGraphTest();
		List<EpisimEvent> events = t.generateEvents();

		Path tmp = Files.createTempFile("events", "bin");

		DataOutputStream out = new DataOutputStream(Files.newOutputStream(tmp, StandardOpenOption.CREATE));

		EpisimEvent.write(out, events, t.infectionParams);

		out.close();

		DataInputStream in = new DataInputStream(Files.newInputStream(tmp));

		List<EpisimEvent> other = EpisimEvent.read(in, ContactGraphTest.persons, ContactGraphTest.container,
				t.infectionParams.toArray(new EpisimConfigGroup.InfectionParams[0]));

		assertThat(tmp.toFile())
				.hasSize(EpisimEvent.sizeOf(events));

		assertThat(other)
				.isEqualTo(events);

	}
}
