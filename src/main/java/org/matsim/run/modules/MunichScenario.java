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
package org.matsim.run.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;

import java.time.*;

/**
 * Scenario based on the publicly available OpenBerlin scenario (https://github.com/matsim-scenarios/matsim-berlin).
 */
public class MunichScenario extends AbstractModule {

	/**
	 * Activity names of the default params from {@link #addDefaultParams(EpisimConfigGroup)}.
	 */
	public static final String[] DEFAULT_ACTIVITIES = {
		"pt", "work", "leisure", "edu", "shop", "errands", "business", "other", "freight", "home"
	};

	/**
	 * Adds default parameters that should be valid for most scenarios.
	 */
	public static void addDefaultParams(EpisimConfigGroup config) {
		// pt
		config.getOrAddContainerParams("pt", "tr");
		// regular out-of-home acts:
		config.getOrAddContainerParams("work");
		config.getOrAddContainerParams("leisure", "leis");
		config.getOrAddContainerParams("edu");
		config.getOrAddContainerParams("shop");
		config.getOrAddContainerParams("errands");
		config.getOrAddContainerParams("business");
		config.getOrAddContainerParams("other");
		// freight act:
		config.getOrAddContainerParams("freight");
		// home act:
		config.getOrAddContainerParams("home");
		config.getOrAddContainerParams("quarantine_home");
	}

	@Provides
	@Singleton
	public Config config() {

		Config config = ConfigUtils.createConfig(new EpisimConfigGroup());
		//Config config = "C:/code/matsim-episim-libs/output/config_noActWeekend.xml";
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		//config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");
		config.network().setInputFile("Z:/indiv/alona/tengos/matsimOutput_qin/output_network.xml.gz");

		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct-schools/output-berlin-v5.4-1pct-schools/berlin-v5.4-1pct-schools.output_events_for_episim.xml.gz";
		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz";
		// String episimEvents_10pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct-schools/output-berlin-v5.4-10pct-schools/berlin-v5.4-10pct-schools.output_events_for_episim.xml.gz";

		//String url = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz";
		String url = "C:/code/matsim-episim-libs/output/downSize1.0_qin_facility_input/output_events_withFacilityId-1.0.xml.gz";
		///String url = "C:/code/matsim-episim-libs/output/inputBerlinFacility/be_2020-week_snz_episim_events_sa_25pt_split.xml.gz";
		//String url = "C:/code/matsim-episim-libs/output/downSize1.0_qin/output_events-1.0.xml.gz";
		episimConfig.setInputEventsFile(url);
		config.facilities().setInputFile("C:/code/matsim-episim-libs/output/downSize1.0_qin_facility_input/facilities1.0.xml.gz");

		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.snz);
		//episimConfig.setSampleSize(0.01);
		episimConfig.setCalibrationParameter(2);
		episimConfig.setInitialInfections(10);
		episimConfig.setMaxContacts(3);
		episimConfig.setStartDate(LocalDate.of(2021, 1, 1));
		episimConfig.setSampleSize(1.0);
		episimConfig.createInitialRestrictions();
		episimConfig.getOrAddContainerParams("home").setContactIntensity(1.0).setSpacesPerFacility(10000); // 33/33

		//episimConfig.setOutputEventsFolder("events");

		long closingIteration = 14;

		addDefaultParams(episimConfig);

		episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
			.restrict(closingIteration, Restriction.of(0.0), "leisure", "edu")
			.restrict(closingIteration, Restriction.of(0.2), "work", "business", "other")
			.restrict(closingIteration, Restriction.of(0.3), "shop", "errands")
			.restrict(closingIteration, Restriction.of(0.5), "pt")
			.restrict(closingIteration + 60, Restriction.of(1.0), DEFAULT_ACTIVITIES)
			.build()
		);

		return config;
	}

}
