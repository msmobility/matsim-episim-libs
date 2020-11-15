package org.matsim.episim;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.junit.Before;
import org.junit.Test;
import org.matsim.episim.data.EpisimEventProvider;
import org.matsim.run.modules.OpenBerlinScenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class EventsFromContactGraphTest {

	private EpisimConfigGroup config;
	private EventsFromMATSimScenario provider;

	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(Modules.override(new EpisimModule()).with(new OpenBerlinScenario()));

		EpisimEventProvider events = injector.getInstance(EpisimEventProvider.class);
		InfectionEventHandler handler = injector.getInstance(InfectionEventHandler.class);

		events.init();
		handler.init(events);

		provider = (EventsFromMATSimScenario) injector.getInstance(EpisimEventProvider.class);
		config = injector.getInstance(EpisimConfigGroup.class);

	}

	@Test
	public void write() throws IOException {

		Path tmp = Files.createTempFile("events", "zip");

		EventsFromContactGraph.writeGraph(tmp, provider, config.getInfectionParams());

		assertThat(tmp)
				.exists()
				.isRegularFile();

	}
}
