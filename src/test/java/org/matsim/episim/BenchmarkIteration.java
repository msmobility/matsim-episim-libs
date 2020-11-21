package org.matsim.episim;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.episim.data.EpisimEventProvider;
import org.matsim.run.modules.SnzBerlinProductionScenario;
import org.matsim.run.modules.SnzBerlinWeekScenario2020;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class BenchmarkIteration {

	private EpisimRunner runner;
	private InfectionEventHandler handler;
	private EpisimEventProvider events;
	private EpisimReporting reporting;
	private int iteration = 1;

	public static void main(String[] args) throws RunnerException {

		Options opt = new OptionsBuilder()
				.include(BenchmarkIteration.class.getSimpleName())
				.warmupIterations(12).warmupTime(TimeValue.seconds(1))
				.measurementIterations(30).measurementTime(TimeValue.seconds(1))
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup
	public void setup() {

		Injector injector = Guice.createInjector(Modules.override(new EpisimModule()).with(new SnzBerlinProductionScenario()));

		//injector.getInstance(EpisimConfigGroup.class).setWriteEvents(EpisimConfigGroup.WriteEvents.tracing);
		//injector.getInstance(TracingConfigGroup.class).setPutTraceablePersonsInQuarantineAfterDay(0);

		runner = injector.getInstance(EpisimRunner.class);
		events = injector.getInstance(EpisimEventProvider.class);
		handler = injector.getInstance(InfectionEventHandler.class);
		reporting = injector.getInstance(EpisimReporting.class);

		// benchmark with event writing
		// injector.getInstance(EventsManager.class).addHandler(reporting);

		events.init();
		handler.init(events);
	}

	@Benchmark
	public void iteration() {

		runner.doStep(events, handler, reporting, iteration);
		iteration++;

	}
}
