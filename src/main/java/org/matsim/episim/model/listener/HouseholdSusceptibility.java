package org.matsim.episim.model.listener;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.episim.EpisimPerson;
import org.matsim.episim.InfectionEventHandler;
import org.matsim.episim.model.SimulationStartListener;
import org.matsim.facilities.ActivityFacility;
import org.matsim.scenarioCreation.DistrictLookup;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class HouseholdSusceptibility implements SimulationStartListener {

	private static final Logger log = LogManager.getLogger(HouseholdSusceptibility.class);

	/**
	 * Susceptibility for each household.
	 */
	private final Object2DoubleMap<String> houseHoldSusceptibility = new Object2DoubleOpenHashMap<>();
	private final Config config;

	@Inject
	public HouseholdSusceptibility(Config config) {
		this.config = config;
	}

	@Override
	public void init(SplittableRandom rnd, Map<Id<Person>, EpisimPerson> persons, Map<Id<ActivityFacility>, InfectionEventHandler.EpisimFacility> facilities, Map<Id<Vehicle>, InfectionEventHandler.EpisimVehicle> vehicles) {

		DistrictLookup.Index index = null;
		if (config.shp != null) {
			CoordinateTransformation ct = new IdentityTransformation();
			try {
				index = new DistrictLookup.Index(config.shp.toFile(), ct, config.feature);
			} catch (IOException e) {
				log.error("Could not read shape file", e);
			}
		}

		int selected = 0;

		for (EpisimPerson p : persons.values()) {

			if (index != null) {

				double homeX = (double) p.getAttributes().getAttribute("homeX");
				double homeY = (double) p.getAttributes().getAttribute("homeY");

				try {
					String result = index.query(homeX, homeY);
					if (!config.selection.contains(result))
						continue;

					selected++;

				} catch (NoSuchElementException e) {
					continue;
				}

			}

			String homeId = getHomeId(p);
			p.setSusceptibility(houseHoldSusceptibility.computeIfAbsent(homeId, (k) -> sample(rnd)));
		}

		if (index != null)
			log.info("Selected {} persons by shape file", selected);
	}

	/**
	 * Samples susceptibility for a household.
	 */
	private double sample(SplittableRandom rnd) {

		if (rnd.nextDouble() < config.pHouseholds)
			return config.susceptibility;

		return 1.0;
	}

	private String getHomeId(EpisimPerson person) {
		String home = (String) person.getAttributes().getAttribute("homeId");
		// fallback to person id if there is no home
		return home != null ? home : person.getPersonId().toString();
	}

	@Override
	public String toString() {
		return "HouseholdSusceptibility{p=" + config.pHouseholds + ", susp=" + config.susceptibility + "}";
	}

	/**
	 * @param pHouseholds    Percentage of households that have a modified susceptibility
	 * @param susceptibility adjusted susceptibility
	 */
	public static Config newConfig(double pHouseholds, double susceptibility) {
		return new Config(pHouseholds, susceptibility);
	}


	/**
	 * Holds config options for this class.
	 */
	public static final class Config {

		private final double pHouseholds;
		private final double susceptibility;

		private Path shp;
		private String feature;
		private Set<String> selection;

		private Config(double pHouseholds, double susceptibility) {
			this.pHouseholds = pHouseholds;
			this.susceptibility = susceptibility;
		}

		/**
		 * Filter by shape file.
		 */
		public Config withShape(Path shp) {
			this.shp = shp;
			return this;
		}

		/**
		 * Filter by {@code featureName} to be contained in {@code values}.
		 */
		public Config withFeature(String featureName, String... values) {
			this.feature = featureName;
			this.selection = new HashSet<>();
			this.selection.addAll(Arrays.asList(values));
			return this;
		}

	}

}
