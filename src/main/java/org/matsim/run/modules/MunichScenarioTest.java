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
import org.matsim.episim.EpisimUtils;
import org.matsim.episim.TracingConfigGroup;
import org.matsim.episim.model.FaceMask;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Scenario based on the publicly available OpenBerlin scenario (https://github.com/matsim-scenarios/matsim-berlin).
 */
public class MunichScenarioTest extends AbstractModule {

	//	MunichV2 has roughly 4.400.000 agents, BerlinV2 has roughly 1.200.000,currently using 5%
	public static final double SCALE_FACTOR_MUNICH_TO_BERLIN = 4_400_000./1_200_000.;

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
		config.getOrAddContainerParams("recreation").setContactIntensity(9.24).setSpacesPerFacility(20).setSeasonal(true);
		config.getOrAddContainerParams("other").setContactIntensity(1.47).setSpacesPerFacility(20);
		config.getOrAddContainerParams("nursing").setContactIntensity(11.).setSpacesPerFacility(20);
		// freight act:
		//config.getOrAddContainerParams("freight");
		// home act:
		config.getOrAddContainerParams("home").setContactIntensity(1.).setSpacesPerFacility(1.);
		config.getOrAddContainerParams("quarantine_home").setContactIntensity(0.3).setSpacesPerFacility(1.);
	}

	//public static String mitoTripFilePath = "C:\\models\\SILO\\muc\\scenOutput\\test_transitAssignmentWithGQ\\2011\\microData\\trips.csv";
	public static String mitoTripFilePath = "\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\TRB\\mito\\base\\trips.csv";
	public static String egoAlterHouseholdFilePath = "C:\\models\\tengos_episim\\input/egoAlterHousehold5pct.csv";
	public static String egoAlterJobFilePath = "C:\\models\\tengos_episim\\input/egoAlterJob5pct.csv";
	public static String egoAlterNursingHomeFilePath = "C:\\models\\tengos_episim\\input/egoAlterNursingHome5pct.csv";
	public static String egoAlterSchoolFilePath = "C:\\models\\tengos_episim\\input/egoAlterSchool5pct.csv";
	public static String egoAlterDwellingFilePath = "C:\\models\\tengos_episim\\input/egoAlterDwelling5pct.csv";
	@Provides
	@Singleton
	public Config config() {

		Config config = ConfigUtils.createConfig(new EpisimConfigGroup());
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		config.global().setRandomSeed(1);

		config.controler().setOutputDirectory("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\calibration\\25pct_7days_0.000_000_45_unconstrained_seed1\\");
		//config.facilities().setInputFile("F:\\models\\tengos_episim\\input/facility_simplified_100mGrid_filtered_ptOnly.xml.gz");

		//if running only one event file for whole week
		//episimConfig.setInputEventsFile("C:\\models\\mito7\\muc\\scenOutput\\tengos_25pct_matsim_saturday\\output_events-1.0_combined3.xml.gz");

		//episimConfig.setInputEventsFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\IATBR2024\\mito\\clique_destination_15perc_pairThenClique_matsim/output_events-1.0.xml.gz");
		//If add event files for different day of week
		episimConfig.addInputEventsFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\episum_input\\output_events_for_episim\\output_events_monday.xml.gz")
				.addDays(DayOfWeek.MONDAY);

		episimConfig.addInputEventsFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\episum_input\\output_events_for_episim\\output_events_tuesday.xml.gz")
				.addDays(DayOfWeek.TUESDAY);

		episimConfig.addInputEventsFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\episum_input\\output_events_for_episim\\output_events_wednesday.xml.gz")
				.addDays(DayOfWeek.WEDNESDAY);

		episimConfig.addInputEventsFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\episum_input\\output_events_for_episim\\output_events_thursday.xml.gz")
				.addDays(DayOfWeek.THURSDAY);

		episimConfig.addInputEventsFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\episum_input\\output_events_for_episim\\output_events_friday.xml.gz")
				.addDays(DayOfWeek.FRIDAY);

		episimConfig.addInputEventsFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\episum_input\\output_events_for_episim\\output_events_saturday.xml.gz")
				.addDays(DayOfWeek.SATURDAY);

		episimConfig.addInputEventsFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\episum_input\\output_events_for_episim\\output_events_sunday.xml.gz")
				.addDays(DayOfWeek.SUNDAY);


		config.network().setInputFile("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\TRB\\mito\\base\\output_network.xml.gz");


		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.bln);//snz: run with facility file
		episimConfig.setInitialInfections(10000);

		//episimConfig.setInitialInfectionDistrict("Munich");
		episimConfig.setSampleSize(1);//100% of the 25% matsim simulation
		episimConfig.setCalibrationParameter(0.000_000_45);//what's this? original value: 0.000_011_0, we set to: 0.000_002_6
		episimConfig.setMaxContacts(3);
		String startDate = "2020-02-16";
		episimConfig.setStartDate(startDate);
		episimConfig.setHospitalFactor(1.6);
		//episimConfig.setEndEarly(true);
		episimConfig.setThreads(8);


		//long closingIteration = 14;
		Map<LocalDate, Integer> importMap = new HashMap<>();

		//scale the disease import according to scenario size (number of agents in relation to berlin scenario)
		double importFactor = 1.0 * SCALE_FACTOR_MUNICH_TO_BERLIN;//divide by 5 currently to reflect its only 5%, original is set to 25% sample

		//idk where 0.9 * import factor came from (berlin), but we need to infect at least one person on the start date. tschlenther, 23 sep 2020
		importMap.put(episimConfig.getStartDate(), Math.max(1, (int) Math.round(0.9 * importFactor)));

		int importOffset = 0;
		importMap = interpolateImport(importMap, importFactor, LocalDate.parse("2020-02-24").plusDays(importOffset),
				LocalDate.parse("2020-03-09").plusDays(importOffset), 0.9, 23.1);
		importMap = interpolateImport(importMap, importFactor, LocalDate.parse("2020-03-09").plusDays(importOffset),
				LocalDate.parse("2020-03-23").plusDays(importOffset), 23.1, 3.9);
		importMap = interpolateImport(importMap, importFactor, LocalDate.parse("2020-03-23").plusDays(importOffset),
				LocalDate.parse("2020-04-13").plusDays(importOffset), 3.9, 0.1);
		importMap = interpolateImport(importMap, importFactor, LocalDate.parse("2020-06-08").plusDays(importOffset),
				LocalDate.parse("2020-07-13").plusDays(importOffset), 0.1, 2.7);
		importMap = interpolateImport(importMap, importFactor, LocalDate.parse("2020-07-13").plusDays(importOffset),
				LocalDate.parse("2020-08-10").plusDays(importOffset), 2.7, 17.9);
		importMap = interpolateImport(importMap, importFactor, LocalDate.parse("2020-08-10").plusDays(importOffset),
				LocalDate.parse("2020-08-24").plusDays(importOffset), 17.9, 8.6);
		importMap.put(LocalDate.parse("2020-08-25").plusDays(importOffset), 0);

		episimConfig.setInfections_pers_per_day(importMap);


		addDefaultParams(episimConfig);



		//comment or uncomment to restrict education and work activities
		FixedPolicy.ConfigBuilder policyBuilder = FixedPolicy.config();

		// Building the final policy
		episimConfig.setPolicy(FixedPolicy.class, policyBuilder.build());

		return config;
	}

	private static Map<LocalDate, Integer> interpolateImport(Map<LocalDate, Integer> importMap, double importFactor, LocalDate start, LocalDate end, double a, double b) {

		int days = end.getDayOfYear() - start.getDayOfYear();

		for (int i = 1; i<=days; i++) {
			double fraction = (double) i / days;
			importMap.put(start.plusDays(i), (int) Math.round(importFactor * (a + fraction * (b-a))));
		}

		return importMap;

	}

}
