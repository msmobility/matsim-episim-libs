package org.matsim.scenarioCreation;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.episim.*;
import org.matsim.episim.data.EpisimEventProvider;
import org.matsim.run.RunEpisim;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
		name = "createStaticGraph",
		description = "Creates static event files for a scenario"
)
public class CreateStaticGraph implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(CreateStaticGraph.class);

	@CommandLine.Parameters(paramLabel = "MODULE", arity = "1..*", description = "List of modules to load (See RunEpisim)")
	private List<String> moduleNames = new ArrayList<>();

	@CommandLine.Option(names = "--output", description = "Output path", defaultValue = "staticEvents.tar.lz4")
	private Path output;

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateStaticGraph()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		Injector injector = Guice.createInjector(Modules.override(new EpisimModule()).with(RunEpisim.resolveModules(moduleNames)));

		EpisimEventProvider events = injector.getInstance(EpisimEventProvider.class);
		InfectionEventHandler handler = injector.getInstance(InfectionEventHandler.class);

		events.init();
		handler.init(events);

		EventsFromMATSimScenario provider = (EventsFromMATSimScenario) injector.getInstance(EpisimEventProvider.class);
		EpisimConfigGroup config = injector.getInstance(EpisimConfigGroup.class);

		EventsFromContactGraph.writeGraph(output, provider, config.getInfectionParams());

		return 0;
	}
}
