package org.matsim.episim.model.input;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

public class CreateRestrictionsFromMobilityDataTest {


	@Ignore
	@Test
	public void create() throws IOException {

		CreateRestrictionsFromMobilityData r = new CreateRestrictionsFromMobilityData();
		r.setInput(Path.of("berlinGoogleMobility.csv"));


		r.createPolicy();

	}
}
