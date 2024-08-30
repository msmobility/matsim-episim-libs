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
package org.matsim.scenarioCreation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static picocli.CommandLine.*;

/**
 * Takes a population and samples a certain percentage of it.
 * As output writes an event file with events only relevant for episim and belonging to the sample.
 */
@Command(
		name = "downSample",
		description = "Down sample scenario and extract information for episim.",
		mixinStandardHelpOptions = true
)
public class DownSampleScenario implements Callable<Integer> {

	private static Logger log = LogManager.getLogger(DownSampleScenario.class);

	@Parameters(paramLabel = "sampleSize", arity = "1", description = "Desired percentage of the sample between (0, 1)", defaultValue = "1")
	private double sampleSize;

	@Option(names = "--population", required = true, description = "Population xml file",defaultValue = "C:\\models\\mito7\\muc\\scenOutput\\tengos_25pct_matsim_sunday\\no_pt\\output_plans.xml.gz")
	private Path population;

	@Option(names = "--output", description = "Output folder", defaultValue = "C:\\models\\mito7\\muc\\scenOutput\\tengos_25pct_matsim_sunday\\no_pt\\")
	private Path output;

	@Option(names = "--events", required = true, description = "Path to events file",defaultValue = "C:\\models\\mito7\\muc\\scenOutput\\tengos_25pct_matsim_sunday\\no_pt\\output_events.xml.gz")
	private List<Path> eventFiles;

	@Option(names = "--facilities", description = "Path to facility file")
	private Path facilities;

	@Option(names = "--seed", defaultValue = "1", description = "Random seed used for sampling")
	private long seed;


	public static void main(String[] args) {
		System.exit(new CommandLine(new DownSampleScenario()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(population)) {
			log.error("Population file {} does not exists", population);
			return 2;
		}

		if (!Files.exists(output)) Files.createDirectories(output);

		log.info("Sampling with size {}", sampleSize);

		MatsimRandom.reset(seed);

		Population population = PopulationUtils.readPopulation(this.population.toString());
		PopulationUtils.sampleDown(population, sampleSize);


		/*Population population2 = PopulationUtils.readPopulation("F:\\models\\mitoMunich\\input\\trafficAssignment\\pt/matsimPlans.xml.gz");
		List<Id<Person>> populationPtList = new ArrayList<>();
		MainModeIdentifierImpl mainModeIdentifier = new MainModeIdentifierImpl();
		for (Person pp : population2.getPersons().values()) {
			String mode = mainModeIdentifier.identifyMainMode(TripStructureUtils.getLegs(pp.getSelectedPlan()));
			if (mode.equals("pt")) {
				populationPtList.add(pp.getId());
			}
		}

		log.warn("Total number of pt agents: "+populationPtList.size() + "|all agents: "+population.getPersons().size());

		Population populationNew = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		for(Id<Person> id: populationPtList){
			populationNew.addPerson(population.getPersons().get(id));
		}

		population=populationNew;

		PopulationUtils.writePopulation(population, output.resolve("populationPt" + sampleSize + ".xml.gz").toString());*/

		if (!eventFiles.stream().allMatch(Files::exists)) {
			log.error("Event files {} do not exists", eventFiles);
			return 2;
		}

		Set<Id<ActivityFacility>> filterFacilities = new HashSet<>();

		for (Path events : eventFiles) {

			log.info("Reading event file {}", events);

			EventsManager manager = EventsUtils.createEventsManager();
			FilterHandler handler = new FilterHandler(population, null, null);
			manager.addHandler(handler);
			EventsUtils.readEvents(manager, events.toString());

			String name = events.getFileName().toString().replace(".xml.gz", "-");
			EventWriterXML writer = new EventWriterXML(
					IOUtils.getOutputStream(IOUtils.getFileUrl(output.resolve(name + sampleSize + ".xml.gz").toString()), false)
			);

			log.info("Filtered {} out of {} events = {}%", handler.getEvents().size(), handler.getCounter(), handler.getEvents().size() / handler.getCounter());

			handler.getEvents().forEach(writer::handleEvent);
			writer.closeFile();

			filterFacilities.addAll(handler.facilities);
		}

		if (facilities == null || !Files.exists(facilities)) {
			log.warn("Facilities file {} does not exist", facilities);
			return 0;
		}

		log.info("Reading {}...", this.facilities);

		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
 		MatsimFacilitiesReader fReader = new MatsimFacilitiesReader(null, null, facilities);
		fReader.parse(IOUtils.getInputStream(IOUtils.getFileUrl(this.facilities.toString())));

		int n = facilities.getFacilities().size();

		Set<Id<ActivityFacility>> toRemove = facilities.getFacilities().keySet()
				.stream().filter(k -> !filterFacilities.contains(k)).collect(Collectors.toSet());

		toRemove.forEach(k -> facilities.getFacilities().remove(k));

		log.info("Filtered {} out of {} facilities", facilities.getFacilities().size(), n);

		new FacilitiesWriter(facilities).write(
				IOUtils.getOutputStream(IOUtils.getFileUrl(output.resolve("facilities" + sampleSize + ".xml.gz").toString()), false)
		);

		return 0;
	}

}
