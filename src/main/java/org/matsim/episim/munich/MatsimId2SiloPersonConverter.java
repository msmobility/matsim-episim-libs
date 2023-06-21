package org.matsim.episim.munich;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.run.modules.MunichScenarioTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatsimId2SiloPersonConverter extends AbstractCsvReader {

	private static final Logger logger = Logger.getLogger(MatsimId2SiloPersonConverter.class);

	private int posTripId = -1;
	private int posSiloPersonId = -1;
	public static Map<Integer,Integer> tripId2PersonId = new HashMap<>();

	public void read() {
		logger.info("  Reading trip-silo person lists from csv file");
		Path filePath = Paths.get(MunichScenarioTest.mitoTripFilePath);
		super.read(filePath, ",");
		logger.info("  Finished " + tripId2PersonId.size());
	}

	@Override
	public void processHeader(String[] header) {
		List<String> headerList = Arrays.asList(header);
		posTripId = headerList.indexOf("id");
		posSiloPersonId = headerList.indexOf("person");
	}

	@Override
	public void processRecord(String[] record) {
		final int tripId = Integer.parseInt(record[posTripId]);
		final int siloPersonId = Integer.parseInt(record[posSiloPersonId]);
		tripId2PersonId.put(tripId,siloPersonId);
	}
}

