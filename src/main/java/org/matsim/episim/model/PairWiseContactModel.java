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
package org.matsim.episim.model;

import com.google.inject.Inject;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.episim.*;

import java.util.*;

import org.matsim.episim.data.DiseaseStatus;
import org.matsim.episim.data.EpisimContainer;
import org.matsim.episim.data.PersonLeavesContainerEvent;


/**
 * Model where persons are only interacting pairwise.
 */
public final class PairWiseContactModel extends AbstractContactModel {

	private static final Logger log = LogManager.getLogger(PairWiseContactModel.class);

	/**
	 * Flag to enable tracking, which is considerably slower.
	 */
	private final int trackingAfterDay;

	/**
	 * Whether to trace susceptible persons.
	 */
	private final boolean traceSusceptible;

	/**
	 * This buffer is used to store the infection type.
	 */
	private final StringBuilder buffer = new StringBuilder();

	/**
	 * Reusable list for contact persons.
	 */
	private final List<MutableEpisimPerson> contactPersons = new ArrayList<>();

	private final Map<EpisimContainer, Set<MutableEpisimPerson>> contacts = new IdentityHashMap<>();

	@Inject
		/*package*/ PairWiseContactModel(SplittableRandom rnd, Config config, TracingConfigGroup tracingConfig,
										 EpisimReporting reporting, InfectionModel infectionModel) {
		super(rnd, config, infectionModel, reporting);
		this.trackingAfterDay = tracingConfig.getPutTraceablePersonsInQuarantineAfterDay();
		this.traceSusceptible = tracingConfig.getTraceSusceptible();
	}

	@Override
	public void infectionDynamicsContainer(PersonLeavesContainerEvent event, double now) {
		throw new NotImplementedException("TODO");
	}
}
