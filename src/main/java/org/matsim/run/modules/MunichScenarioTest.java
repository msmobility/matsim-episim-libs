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

import java.time.DayOfWeek;

/**
 * Scenario based on the publicly available OpenBerlin scenario (https://github.com/matsim-scenarios/matsim-berlin).
 */
public class MunichScenarioTest extends AbstractModule {

	/**
	 * Activity names of the default params from {@link #addDefaultParams(EpisimConfigGroup)}.
	 */
	public static final String[] DEFAULT_ACTIVITIES = {
			"pt", "work", "education", "shopping", "other", "home"
	};

	/**
	 * Adds default parameters that should be valid for most scenarios.
	 */
/*	public static void addDefaultParams(EpisimConfigGroup config) {
		// pt
		config.getOrAddContainerParams("pt", "tr").setContactIntensity(10.).setSpacesPerFacility(20);
		// regular out-of-home acts:
		config.getOrAddContainerParams("work").setContactIntensity(1.47).setSpacesPerFacility(20);
		config.getOrAddContainerParams("education").setContactIntensity(11.).setSpacesPerFacility(20);
		config.getOrAddContainerParams("shopping").setContactIntensity(0.88).setSpacesPerFacility(20);
		config.getOrAddContainerParams("other").setContactIntensity(9.24).setSpacesPerFacility(20);
		// freight act:
		//config.getOrAddContainerParams("freight");
		// home act:
		config.getOrAddContainerParams("home").setContactIntensity(1.).setSpacesPerFacility(1.);
		config.getOrAddContainerParams("quarantine_home").setContactIntensity(1.).setSpacesPerFacility(1.);
	}*/

	public static void addDefaultParams(EpisimConfigGroup config) {
		// pt
		config.getOrAddContainerParams("pt", "tr").setContactIntensity(10.).setSpacesPerFacility(20);
		// regular out-of-home acts:
		config.getOrAddContainerParams("work").setContactIntensity(1.47).setSpacesPerFacility(20);
		config.getOrAddContainerParams("education").setContactIntensity(11.).setSpacesPerFacility(20);
		config.getOrAddContainerParams("shopping").setContactIntensity(0.88).setSpacesPerFacility(20);
		config.getOrAddContainerParams("recreation").setContactIntensity(9.24).setSpacesPerFacility(20);
		config.getOrAddContainerParams("other").setContactIntensity(1.47).setSpacesPerFacility(20);
		config.getOrAddContainerParams("nursing").setContactIntensity(11.).setSpacesPerFacility(20);
		// freight act:
		//config.getOrAddContainerParams("freight");
		// home act:
		config.getOrAddContainerParams("home").setContactIntensity(1.).setSpacesPerFacility(1.);
		config.getOrAddContainerParams("quarantine_home").setContactIntensity(1.).setSpacesPerFacility(1.);
	}

	public static String mitoTripFilePath = "F:\\models\\tengos_episim\\input\\mito_coordinated/trips.csv";
	public static String egoAlterHouseholdFilePath = "F:\\models\\tengos_episim\\input\\mito_coordinated/egoAlterHousehold5pct.csv";
	public static String egoAlterJobFilePath = "F:\\models\\tengos_episim\\input\\mito_coordinated/egoAlterJob5pct.csv";
	public static String egoAlterNursingHomeFilePath = "F:\\models\\tengos_episim\\input\\mito_coordinated/egoAlterNursingHome5pct.csv";
	public static String egoAlterSchoolFilePath = "F:\\models\\tengos_episim\\input\\mito_coordinated/egoAlterSchool5pct.csv";
	public static String egoAlterDwellingFilePath = "F:\\models\\tengos_episim\\input\\mito_coordinated/egoAlterDwelling5pct.csv";
	@Provides
	@Singleton
	public Config config() {

		Config config = ConfigUtils.createConfig(new EpisimConfigGroup());
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		config.controler().setOutputDirectory("F:\\models\\tengos_episim\\scenOutput\\test");
		//config.facilities().setInputFile("F:\\models\\tengos_episim\\input/facility_simplified_100mGrid_filtered_ptOnly.xml.gz");
		episimConfig.setInputEventsFile("F:\\models\\tengos_episim\\input\\mito_coordinated/output_events-1.0.xml.gz");

		//If add event files for different day of week
		//episimConfig.addInputEventsFile("be_2020-week_snz_episim_events_wt_%dpt_split.xml.gz")
		//		.addDays(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

		config.network().setInputFile("F:\\models\\tengos_episim\\input\\mito_coordinated/output_network.xml.gz");

		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.bln);//snz: run with facility file
		episimConfig.setInitialInfections(500);
		//episimConfig.setInitialInfectionDistrict("Munich");
		episimConfig.setSampleSize(1);//100% of the 5% matsim simulation
		episimConfig.setCalibrationParameter(0.000_011_0);//what's this?0.000_002_6
		episimConfig.setMaxContacts(3);
		String startDate = "2020-02-16";
		episimConfig.setStartDate(startDate);
		episimConfig.setHospitalFactor(1.6);

		//long closingIteration = 14;

		addDefaultParams(episimConfig);

		/*episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
				.restrict(closingIteration, Restriction.of(0.0),  "education")
				.restrict(closingIteration, Restriction.of(0.2), "work", "other")
				.restrict(closingIteration, Restriction.of(0.3), "shopping")
				.restrict(closingIteration, Restriction.of(0.5), "pt")
				.restrict(closingIteration + 60, Restriction.of(1.0), DEFAULT_ACTIVITIES)
				.build()
		);*/

		return config;
	}

}
