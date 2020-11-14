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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.typesafe.config.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.data.*;
import org.matsim.episim.model.ContactModel;
import org.matsim.episim.model.InitialInfectionHandler;
import org.matsim.episim.model.ProgressionModel;
import org.matsim.episim.policy.Restriction;
import org.matsim.episim.policy.ShutdownPolicy;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.DayOfWeek;
import java.util.*;

import static org.matsim.episim.EpisimUtils.readChars;
import static org.matsim.episim.EpisimUtils.writeChars;

/**
 * Main event handler of episim.
 * It consumes the events of a standard MATSim run and puts {@link MutableEpisimPerson}s into {@link MutableEpisimContainer}s during their activity.
 * At the end of activities an {@link ContactModel} is executed and also a {@link ProgressionModel} at the end of the day.
 * See {@link EpisimModule} for which components may be substituted.
 */
public final class InfectionEventHandler implements Externalizable {
	// Some notes:

	// * Especially if we repeat the same events file, then we do not have complete mixing.  So it may happen that only some subpopulations gets infected.

	// * However, if with infection proba=1 almost everybody gets infected, then in our current setup (where infected people remain in the iterations),
	// this will also happen with lower probabilities, albeit slower.  This is presumably the case that we want to investigate.

	// * We seem to be getting two different exponential spreading rates.  With infection proba=1, the crossover is (currently) around 15h.

	// TODO

	// * yyyyyy There are now some things that depend on ID conventions.  We should try to replace them.  This presumably would mean to interpret
	//  additional events.  Those would need to be prepared for the "reduced" files.  kai, mar'20

	private static final Logger log = LogManager.getLogger(InfectionEventHandler.class);


	/**
	 * Holds the current restrictions in place for all the activities.
	 */
	private final Map<String, Restriction> restrictions;

	/**
	 * Policy that will be enforced at the end of each day.
	 */
	private final ShutdownPolicy policy;

	/**
	 * Progress of the sickness at the end of the day.
	 */
	private final ProgressionModel progressionModel;

	/**
	 * Models the process of persons infecting each other during activities.
	 */
	private final ContactModel contactModel;

	/**
	 * Handle initial infections.
	 */
	private final InitialInfectionHandler initialInfections;

	/**
	 * Scenario with population information.
	 */
	private final Scenario scenario;

	/**
	 * Local random, e.g. used for person initialization.
	 */
	private final SplittableRandom localRnd;

	private final Config config;
	private final EpisimConfigGroup episimConfig;
	private final TracingConfigGroup tracingConfig;
	private final EpisimReporting reporting;
	private final SplittableRandom rnd;

	/**
	 * All persons.
	 */
	private Map<Id<Person>, MutableEpisimPerson> personMap = new IdMap<>(Person.class);

	/**
	 * All container.
	 */
	private Set<EpisimContainer> container;

	private boolean init = false;
	private int iteration = 0;

	/**
	 * Most recent infection report for all persons.
	 */
	private EpisimReporting.InfectionReport report;

	@Inject
	public InfectionEventHandler(Config config, Scenario scenario, ProgressionModel progressionModel, EpisimReporting reporting,
								 InitialInfectionHandler initialInfections, ContactModel contactModel, SplittableRandom rnd) {
		this.config = config;
		this.episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		this.tracingConfig = ConfigUtils.addOrGetModule(config, TracingConfigGroup.class);
		this.scenario = scenario;
		this.policy = episimConfig.createPolicyInstance();
		this.restrictions = episimConfig.createInitialRestrictions();
		this.reporting = reporting;
		this.rnd = rnd;
		this.localRnd = new SplittableRandom(config.global().getRandomSeed() + 65536);
		this.progressionModel = progressionModel;
		this.contactModel = contactModel;
		this.initialInfections = initialInfections;
		this.initialInfections.setInfectionsLeft(episimConfig.getInitialInfections());
	}



	/**
	 * Returns the last {@link EpisimReporting.InfectionReport}.
	 */
	public EpisimReporting.InfectionReport getReport() {
		return report;
	}

	/**
	 * Returns true if more iterations won't change the results anymore and the simulation is finished.
	 */
	public boolean isFinished() {
		return iteration > 0 && !progressionModel.canProgress(report);
	}

	/**
	 * Initializes all needed data structures before the simulation can start.
	 * This *always* needs to be called before starting.
	 */
	void init(EpisimEventProvider provider) {

		for (Id<Person> id : provider.getPersonIds()) {
			personMap.put(id, createPerson(id));
		}


		container = provider.getContainer();
		policy.init(episimConfig.getStartDate(), ImmutableMap.copyOf(this.restrictions));
		init = true;
	}

	/**
	 * Create a new person and lookup attributes from scenario.
	 */
	private MutableEpisimPerson createPerson(Id<Person> id) {

		Person person = scenario.getPopulation().getPersons().get(id);
		Attributes attrs;
		if (person != null) {
			attrs = person.getAttributes();
		} else {
			attrs = new Attributes();
		}

		boolean traceable = localRnd.nextDouble() < tracingConfig.getEquipmentRate();

		return new MutableEpisimPerson(id, attrs, traceable, reporting);
	}

	/**
	 * Called *before* the start of an iteration.
	 * @param iteration iteration to start
	 */
	public void reset(int iteration) {

		// safety checks
		if (!init)
			throw new IllegalStateException(".init() was not called!");
		if (iteration <= 0)
			throw new IllegalArgumentException("Iteration must be larger 1!");

		DayOfWeek day = EpisimUtils.getDayOfWeek(episimConfig.getStartDate(), iteration);

		progressionModel.setIteration(iteration);
		progressionModel.beforeStateUpdates(personMap, iteration, this.report);

		for (MutableEpisimPerson person : personMap.values()) {
			progressionModel.updateState(person, iteration);
		}

		this.iteration = iteration;

		this.initialInfections.handleInfections(personMap, iteration);

		Map<String, EpisimReporting.InfectionReport> reports = reporting.createReports(personMap.values(), iteration);
		this.report = reports.get("total");

		reporting.reporting(reports, iteration, report.date);
		reporting.reportTimeUse(restrictions.keySet(), personMap.values(), iteration, report.date);

		ImmutableMap<String, Restriction> im = ImmutableMap.copyOf(this.restrictions);
		policy.updateRestrictions(report, im);
		contactModel.setIteration(iteration, personMap, im);
		reporting.reportRestrictions(restrictions, iteration, report.date);

	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		out.writeLong(EpisimUtils.getSeed(rnd));
		out.writeInt(initialInfections.getInfectionsLeft());
		out.writeInt(iteration);

		out.writeInt(restrictions.size());
		for (Map.Entry<String, Restriction> e : restrictions.entrySet()) {
			writeChars(out, e.getKey());
			writeChars(out, e.getValue().asMap().toString());
		}

		out.writeInt(personMap.size());
		for (Map.Entry<Id<Person>, MutableEpisimPerson> e : personMap.entrySet()) {
			writeChars(out, e.getKey().toString());
			e.getValue().write(out);
		}

		out.writeInt(container.size());
		for (EpisimContainer e : container) {
			writeChars(out, e.getContainerId().toString());
			e.write(out);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException {

		long storedSeed = in.readLong();
		if (episimConfig.getSnapshotSeed() == EpisimConfigGroup.SnapshotSeed.restore) {
			EpisimUtils.setSeed(rnd, storedSeed);
		} else if (episimConfig.getSnapshotSeed() == EpisimConfigGroup.SnapshotSeed.reseed) {
			log.info("Reseeding snapshot with {}", config.global().getRandomSeed());
			EpisimUtils.setSeed(rnd, config.global().getRandomSeed());
		}

		initialInfections.setInfectionsLeft(in.readInt());
		iteration = in.readInt();

		int r = in.readInt();
		for (int i = 0; i < r; i++) {
			String act = readChars(in);
			restrictions.put(act, Restriction.fromConfig(ConfigFactory.parseString(readChars(in))));
		}

		// TODO
		// TODO ################

		int persons = in.readInt();
		for (int i = 0; i < persons; i++) {
			Id<Person> id = Id.create(readChars(in), Person.class);
		//	personMap.get(id).read(in, personMap, containerMap);
		}

		int vehicles = in.readInt();
		for (int i = 0; i < vehicles; i++) {
			Id<EpisimContainer> id = Id.create(readChars(in), EpisimContainer.class);
		//	containerMap.get(id).read(in, personMap);
		}
	}


	/**
	 * Processes an episim specific event.
	 */
	void processEvent(EpisimEvent e) {

		double now = EpisimUtils.getCorrectedTime(episimConfig.getStartOffset(), e.getTime(), iteration);

		if (e instanceof PersonEntersContainerEvent) {
			contactModel.notifyEnterContainer((PersonEntersContainerEvent) e, now);
		} else if (e instanceof PersonLeavesContainerEvent) {
			contactModel.infectionDynamicsContainer((PersonLeavesContainerEvent) e, now);
		}

	}
}

