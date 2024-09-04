package org.matsim.episim.munich;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimPerson;
import org.matsim.run.modules.MunichScenarioTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SocialNetworkReader extends AbstractCsvReader {

	private static final Logger logger = Logger.getLogger(SocialNetworkReader.class);

	private int posEgo = -1;
	private int posAlter = -1;
	Map<Integer,SiloPerson> persons = new HashMap<>();
	private int countError;

	public SocialNetworkReader(Map<Integer,SiloPerson> persons) {
		this.persons = persons;
	}

	public void read() {
//		logger.info("  Reading ego-alter household data from csv file");
//		Path filePath = Paths.get(MunichScenarioTest.egoAlterHouseholdFilePath);
//		super.read(filePath, ",");
//		logger.info(countError + " Egos are not existed in the trip person map.");
//		logger.info("  Reading ego-alter job data from csv file");
//		filePath = Paths.get(MunichScenarioTest.egoAlterJobFilePath);
//		super.read(filePath, ",");
//		logger.info(countError + " Egos are not existed in the trip person map.");
//		logger.info("  Reading ego-alter nursing home data from csv file");
//		filePath = Paths.get(MunichScenarioTest.egoAlterNursingHomeFilePath);
//		super.read(filePath, ",");
//		logger.info(countError + " Egos are not existed in the trip person map.");
//		logger.info("  Reading ego-alter school data from csv file");
//		filePath = Paths.get(MunichScenarioTest.egoAlterSchoolFilePath);
//		super.read(filePath, ",");
//		logger.info(countError + " Egos are not existed in the trip person map.");
//		logger.info("  Reading ego-alter dwelling data from csv filemum");
//		filePath = Paths.get(MunichScenarioTest.egoAlterDwellingFilePath);
//		super.read(filePath, ",");
//		logger.info(countError + " Egos are not existed in the trip person map.");
		logger.info("  Reading ego-alter friend data from csv file");
		Path filePath = Paths.get("\\\\nas.ads.mwn.de\\tubv\\mob\\projects\\2021\\tengos\\data\\episum_input\\social_network_25pct\\egoAlterFriends25pct_reflected.csv");
		super.read(filePath, ",");
		logger.info(countError + " Egos do not exist in the trip person map.");
	}

	@Override
	public void processHeader(String[] header) {
		List<String> headerList = Arrays.asList(header);
		posEgo = headerList.indexOf("ego");
		posAlter = headerList.indexOf("alter");
	}

	@Override
	public void processRecord(String[] record) {
		final int ego = Integer.parseInt(record[posEgo]);
		final int alter = Integer.parseInt(record[posAlter]);
		if(persons.get(ego)==null){
			countError++;
			//logger.error("Ego: " + ego + " is not in the person map!");

		}else {
			persons.get(ego).getAlterLists().add(alter);
		}

	}
}

