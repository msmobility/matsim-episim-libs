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
import org.matsim.episim.model.FaceMask;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
		config.getOrAddContainerParams("recreation").setContactIntensity(9.24).setSpacesPerFacility(20);
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

		config.controler().setOutputDirectory("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\calibration\\25pct_7days_0.000_000_6\\");
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
		episimConfig.setCalibrationParameter(0.000_000_6);//what's this? original value: 0.000_011_0, we set to: 0.000_002_6
		episimConfig.setMaxContacts(3);
		String startDate = "2020-02-16";
		episimConfig.setStartDate(startDate);
		episimConfig.setHospitalFactor(1.6);
		//episimConfig.setEndEarly(true);

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

		double maskCompliance = 0.95;
		long introductionPeriod = 14;
		LocalDate masksCenterDate = LocalDate.of(2020, 4, 27);
		double clothFraction = maskCompliance * 0.9;
		double surgicalFraction = maskCompliance * 0.1;

		//comment or uncomment to restrict education and work activities
		FixedPolicy.ConfigBuilder policyBuilder = FixedPolicy.config()
				//.restrict(30, Restriction.of(0.1), "education")
				//.restrict("2020-03-14", Restriction.of(0.3), "work")
				//.restrict(30, Restriction.of(0.3), "shopping","other","recreation")
						.restrict("2020-03-14", 0.0, "nursing")
						.restrict("2020-03-14", 0.0, "education")
						.restrict("2020-05-11", 0.3, "education","nursing")
						.restrict("2020-07-25", 0.2, "education","nursing")

						//Ende der Sommerferien
						.restrict("2020-09-08", 1., "education","nursing")
						//Herbstferien
						.restrict("2020-10-31", 0.2, "education","nursing")
						.restrict("2020-11-08", 1., "education")

						.restrict("2020-11-18", 0.2, "education")
						.restrict("2020-11-19", 1., "education")
						//Weihnachtsferien and second lockdown
						.restrict("2020-12-15", 0.2, "education")
						.restrict("2021-02-21", 1., "education")
						//Osterferien
						.restrict("2021-03-29", 0.2, "education")
						.restrict("2021-04-11", 1., "education")

				//restrictions from google mobility data for bavaria
				.restrict("2020-02-28", 0.99, "recreation")
				.restrict("2020-03-06", 0.98, "recreation")
				.restrict("2020-03-13", 0.73, "recreation")
				.restrict("2020-03-20", 0.28, "recreation")
				.restrict("2020-03-27", 0.33, "recreation")
				.restrict("2020-04-03", 0.37, "recreation")
				.restrict("2020-04-10", 0.35, "recreation")
				.restrict("2020-04-17", 0.41, "recreation")
				.restrict("2020-04-24", 0.44, "recreation")
				.restrict("2020-05-01", 0.56, "recreation")
				.restrict("2020-05-08", 0.61, "recreation")
				.restrict("2020-05-15", 0.72, "recreation")
				.restrict("2020-06-19", 0.9, "recreation")
				.restrict("2020-06-26", 0.92, "recreation")
				.restrict("2020-07-17", 1.0, "recreation")
				.restrict("2020-07-24", 0.99, "recreation")
				.restrict("2020-07-31", 0.97, "recreation")
				.restrict("2020-08-07", 0.96, "recreation")
				.restrict("2020-08-14", 0.93, "recreation")
				.restrict("2020-09-18", 0.95, "recreation")
				.restrict("2020-09-25", 0.92, "recreation")
				.restrict("2020-10-02", 0.88, "recreation")
				.restrict("2020-10-16", 0.86, "recreation")
				.restrict("2020-10-23", 0.84, "recreation")
				.restrict("2020-10-30", 0.72, "recreation")
				.restrict("2020-11-06", 0.67, "recreation")
				.restrict("2020-12-04", 0.66, "recreation")
				.restrict("2020-12-18", 0.37, "recreation")
				.restrict("2020-12-25", 0.35, "recreation")

				.restrict("2020-03-20", 0.72, "shopping")
				.restrict("2020-03-27", 0.79, "shopping")
				.restrict("2020-04-03", 0.82, "shopping")
				.restrict("2020-04-10", 0.79, "shopping")
				.restrict("2020-04-17", 0.89, "shopping")
				.restrict("2020-04-24", 0.86, "shopping")
				.restrict("2020-05-01", 0.99, "shopping")
				.restrict("2020-05-08", 0.96, "shopping")
				.restrict("2020-05-15", 0.94, "shopping")
				.restrict("2020-05-22", 0.98, "shopping")
				.restrict("2020-05-29", 0.92, "shopping")
				.restrict("2020-06-05", 0.86, "shopping")
				.restrict("2020-06-12", 0.94, "shopping")
				.restrict("2020-06-19", 1.0, "shopping")
				.restrict("2020-07-31", 0.98, "shopping")
				.restrict("2020-08-07", 0.99, "shopping")
				.restrict("2020-08-14", 0.88, "shopping")
				.restrict("2020-08-21", 0.94, "shopping")
				.restrict("2020-08-28", 0.93, "shopping")
				.restrict("2020-09-25", 1.0, "shopping")
				.restrict("2020-10-02", 0.94, "shopping")
				.restrict("2020-10-09", 1.0, "shopping")
				.restrict("2020-10-16", 0.99, "shopping")
				.restrict("2020-10-30", 0.92, "shopping")
				.restrict("2020-11-06", 0.95, "shopping")
				.restrict("2020-11-20", 0.94, "shopping")
				.restrict("2020-12-18", 0.8, "shopping")
				.restrict("2020-12-25", 0.75, "shopping")

				.restrict("2020-03-13", 0.7, "accompany","other")
				.restrict("2020-03-20", 0.5, "accompany","other")
				.restrict("2020-04-03", 0.6, "accompany","other")
				.restrict("2020-04-17", 0.65, "accompany","other")
				.restrict("2020-05-01", 0.7, "accompany","other")
				.restrict("2020-05-15", 0.75, "accompany","other")
				.restrict("2020-05-22", 0.8, "accompany","other")
				.restrict("2020-06-05", 0.9, "accompany","other")
				.restrict("2020-06-12", 1.0, "accompany","other")
				.restrict("2020-10-09", 0.8, "accompany","other")

				.restrict("2020-02-21", 0.86, "work")
				.restrict("2020-02-28", 0.99, "work")
				.restrict("2020-03-06", 0.98, "work")
				.restrict("2020-03-13", 0.78, "work")
				.restrict("2020-03-20", 0.56, "work")
				.restrict("2020-04-03", 0.5, "work")
				.restrict("2020-04-17", 0.64, "work")
				.restrict("2020-04-24", 0.61, "work")
				.restrict("2020-05-01", 0.71, "work")
				.restrict("2020-05-08", 0.76, "work")
				.restrict("2020-05-15", 0.67, "work")
				.restrict("2020-05-22", 0.78, "work")
				.restrict("2020-05-29", 0.7, "work")
				.restrict("2020-06-05", 0.65, "work")
				.restrict("2020-06-12", 0.82, "work")
				.restrict("2020-06-19", 0.85, "work")
				.restrict("2020-07-24", 0.78, "work")
				.restrict("2020-07-31", 0.73, "work")
				.restrict("2020-08-07", 0.7, "work")
				.restrict("2020-08-14", 0.67, "work")
				.restrict("2020-08-21", 0.72, "work")
				.restrict("2020-08-28", 0.75, "work")
				.restrict("2020-09-04", 0.81, "work")
				.restrict("2020-09-25", 0.87, "work")
				.restrict("2020-10-02", 0.83, "work")
				.restrict("2020-10-09", 0.88, "work")
				.restrict("2020-10-30", 0.76, "work")
				.restrict("2020-12-11", 0.75, "work")
				.restrict("2020-12-18", 0.48, "work")
				.restrict("2020-12-25", 0.4, "work")

				.restrict("2020-02-21", 0.93, "pt")
				.restrict("2020-02-28", 0.96, "pt")
				.restrict("2020-03-06", 0.91, "pt")
				.restrict("2020-03-13", 0.63, "pt")
				.restrict("2020-03-20", 0.34, "pt")
				.restrict("2020-03-27", 0.37, "pt")
				.restrict("2020-04-03", 0.4, "pt")
				.restrict("2020-04-17", 0.47, "pt")
				.restrict("2020-04-24", 0.49, "pt")
				.restrict("2020-05-01", 0.56, "pt")
				.restrict("2020-05-08", 0.58, "pt")
				.restrict("2020-05-15", 0.64, "pt")
				.restrict("2020-05-29", 0.67, "pt")
				.restrict("2020-06-05", 0.61, "pt")
				.restrict("2020-06-12", 0.71, "pt")
				.restrict("2020-06-19", 0.76, "pt")
				.restrict("2020-06-26", 0.79, "pt")
				.restrict("2020-07-03", 0.82, "pt")
				.restrict("2020-07-10", 0.81, "pt")
				.restrict("2020-07-17", 0.85, "pt")
				.restrict("2020-07-24", 0.82, "pt")
				.restrict("2020-07-31", 0.79, "pt")
				.restrict("2020-08-07", 0.77, "pt")
				.restrict("2020-08-14", 0.76, "pt")
				.restrict("2020-08-21", 0.74, "pt")
				.restrict("2020-08-28", 0.75, "pt")
				.restrict("2020-09-04", 0.86, "pt")
				.restrict("2020-09-11", 0.88, "pt")
				.restrict("2020-09-18", 0.85, "pt")
				.restrict("2020-09-25", 0.81, "pt")
				.restrict("2020-10-02", 0.82, "pt")
				.restrict("2020-10-09", 0.8, "pt")
				.restrict("2020-10-16", 0.78, "pt")
				.restrict("2020-10-23", 0.74, "pt")
				.restrict("2020-10-30", 0.63, "pt")
				.restrict("2020-11-06", 0.66, "pt")
				.restrict("2020-12-11", 0.56, "pt")
				.restrict("2020-12-18", 0.41, "pt")
				.restrict("2020-12-25", 0.39, "pt")




				//restrictions from chart in Berlin scenario calibration paper
/*
						.restrict("2020-03-13", 0.8, "work")
						.restrict("2020-03-20", 0.6, "work")
						.restrict("2020-04-17", 0.65, "work")
						.restrict("2020-05-08", 0.8, "work")
						.restrict("2020-06-05", 0.85, "work")
						.restrict("2020-06-19", 0.9, "work")
						.restrict("2020-06-26", 1.0, "work")
						.restrict("2020-07-03", 0.8, "work")
						.restrict("2020-08-07", 0.9, "work")
						.restrict("2020-10-09", 0.8, "work")


						.restrict("2020-03-13", 0.7, "accompany","other")
						.restrict("2020-03-20", 0.5, "accompany","other")
						.restrict("2020-04-03", 0.6, "accompany","other")
						.restrict("2020-04-17", 0.65, "accompany","other")
						.restrict("2020-05-01", 0.7, "accompany","other")
						.restrict("2020-05-15", 0.75, "accompany","other")
						.restrict("2020-05-22", 0.8, "accompany","other")
						.restrict("2020-06-05", 0.9, "accompany","other")
						.restrict("2020-06-12", 1.0, "accompany","other")
						.restrict("2020-10-09", 0.8, "accompany","other")

						.restrict("2020-02-28", 0.9, "recreation")
						.restrict("2020-03-13", 0.8, "recreation")
						.restrict("2020-03-20", 0.65, "recreation")
						.restrict("2020-03-27", 0.7, "recreation")
						.restrict("2020-04-03", 0.8, "recreation")
						.restrict("2020-05-15", 0.85, "recreation")
						.restrict("2020-06-05", 0.9, "recreation")
						.restrict("2020-06-19", 1.0, "recreation")
						.restrict("2020-10-02", 0.9, "recreation")
						.restrict("2020-10-30", 0.8, "recreation")

						.restrict("2020-03-06", 0.95, "shopping")
						.restrict("2020-03-13", 0.8, "shopping")
						.restrict("2020-03-20", 0.6, "shopping")
						.restrict("2020-04-03", 0.7, "shopping")
						.restrict("2020-04-17", 0.75, "shopping")
						.restrict("2020-04-24", 0.8, "shopping")
						.restrict("2020-05-15", 0.82, "shopping")
						.restrict("2020-05-22", 0.85, "shopping")
						.restrict("2020-05-29", 0.9, "shopping")
						.restrict("2020-06-05", 0.95, "shopping")
						.restrict("2020-06-12", 1.0, "shopping")
						.restrict("2020-10-02", 0.95, "shopping")
						.restrict("2020-10-09", 0.9, "shopping")
						.restrict("2020-10-23", 0.85, "shopping")*/

				.restrict("2020-08-08", Restriction.ofCiCorrection(0.5), "education");

/*				.restrict(LocalDate.parse("2020-04-27"), Restriction.ofMask(Map.of(
						FaceMask.CLOTH, 0.8,
						FaceMask.N95, 0.1,
						FaceMask.SURGICAL, 0.1)), "pt", "shopping", "recreation","other")*/

		// this is the date when it was officially introduced in Berlin, so for the time being we do not make this configurable.  Might be different
		// in MUC and elsewhere!
		//MUC started on the same date https://www.muenchen.de/aktuell/2020-04/corona-einkaufen-maerkte-muenchen-mit-maske.html


		// Adding mask compliance over the introduction period
		for (int ii = 0; ii <= introductionPeriod; ii++) {
			LocalDate date = masksCenterDate.plusDays(-introductionPeriod / 2 + ii);
			policyBuilder.restrict(date, Restriction.ofMask(Map.of(
					FaceMask.CLOTH, clothFraction * ii / introductionPeriod,
					FaceMask.SURGICAL, surgicalFraction * ii / introductionPeriod)), "pt", "shopping");
		}

		// Continuing mask compliance restrictions
		policyBuilder.restrict("2020-06-01", Restriction.ofMask(Map.of(
						FaceMask.CLOTH, 0.8 * 0.9,
						FaceMask.SURGICAL, 0.8 * 0.1,
						FaceMask.N95, 0.8*0.1)), "pt", "shopping")
				.restrict("2020-07-01", Restriction.ofMask(Map.of(
						FaceMask.CLOTH, 0.85 * 0.9,
						FaceMask.SURGICAL, 0.85 * 0.1,
						FaceMask.N95, 0.85*0.1)), "pt", "shopping")
				.restrict("2020-08-01", Restriction.ofMask(Map.of(
						FaceMask.CLOTH, 0.9 * 0.9,
						FaceMask.SURGICAL, 0.9 * 0.1,
						FaceMask.N95, 0.9*0.1)), "pt", "shopping");

		// Building the final policy
		episimConfig.setPolicy(FixedPolicy.class, policyBuilder.build());

/*				.restrict("2020-03-07", Restriction.ofCiCorrection(1), DEFAULT_ACTIVITIES)
				.restrict("2020-03-07", Restriction.ofCiCorrection(1), "quarantine_home")
				.restrict("2020-03-07", Restriction.ofCiCorrection(0.5), "pt")
				.restrict("2020-08-08", Restriction.ofCiCorrection( 0.5), "education")*/







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
