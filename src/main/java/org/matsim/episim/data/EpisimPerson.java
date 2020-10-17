package org.matsim.episim.data;

import com.google.common.annotations.Beta;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * Interface for accessing information about a person in episim simulation.
 */
public interface EpisimPerson extends Attributable {

	/**
	 * Unique id of the person.
	 */
	Id<Person> getId();

	/**
	 * Current {@link DiseaseStatus}.
	 */
	DiseaseStatus getDiseaseStatus();

	/**
	 * Current {@link QuarantineStatus}
	 */
	QuarantineStatus getQuarantineStatus();

	/**
	 * Days elapsed since a certain status was set.
	 * This will always round the change as if it happened on the start of a day.
	 *
	 * @param status     requested status
	 * @param currentDay current day (iteration)
	 * @throws IllegalStateException when the requested status was never set
	 */
	int daysSince(DiseaseStatus status, int currentDay);

	/**
	 * Return whether a person had (or currently has) a certain disease status.
	 */
	boolean hadDiseaseStatus(DiseaseStatus status);

	/**
	 * Days elapsed since person was put into quarantine.
	 *
	 * @param currentDay current day (iteration)
	 * @apiNote This is currently not used much and may change similar to {@link #daysSince(DiseaseStatus, int)}.
	 */
	@Beta
	int daysSinceQuarantine(int currentDay);

	/**
	 * Returns whether the person can be traced.
	 */
	boolean isTraceable();

	/**
	 * Container of infection (if any happened)
	 */
	//Id<EpisimContainer> getInfectionContainer();

	/**
	 * Type of infection, i.e. activities performed when person was infected.
	 */
	String getInfectionType();

	/**
	 * Activity performed by a person. Holds the type and its infection params.
	 */
	final class Activity {

		public final String actType;
		public final EpisimConfigGroup.InfectionParams params;

		/**
		 * Constructor.
		 */
		public Activity(String actType, EpisimConfigGroup.InfectionParams params) {
			this.actType = actType;
			this.params = params;
		}

		@Override
		public String toString() {
			return "Activity{" +
					"actType='" + actType + '\'' +
					'}';
		}
	}
}
