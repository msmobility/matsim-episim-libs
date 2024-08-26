package org.matsim.run.batch;

import com.google.inject.AbstractModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.*;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;
import org.matsim.run.RunParallel;
import org.matsim.run.modules.MunichScenarioTest;

import javax.annotation.Nullable;

/**
 * Calibration for Munich scenario
 */
public class MunichCalibration implements BatchRun<MunichCalibration.Params> {

	@Override
	public AbstractModule getBindings(int id, @Nullable Params params) {
		return new MunichScenarioTest();
	}

	@Override
	public Metadata getMetadata() {
		return Metadata.of("munich", "calibration");
	}

	@Override
	public Config prepareConfig(int id, Params params) {

		MunichScenarioTest module = new MunichScenarioTest();

		Config config = module.config();
		config.global().setRandomSeed(params.seed);

		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		episimConfig.setCalibrationParameter(params.calibrationParam);

/*		episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
				.restrict((long) params.restrictionDay, Restriction.of(0.1), "education")
				.restrict((long) params.restrictionDay, Restriction.of(0.1), "work").build());*/

		return config;
	}

	public static final class Params {

		@GenerateSeeds(2)
		public long seed;

		@Parameter({0.0000016,0.0000014,0.0000012,0.0000010,0.0000008,0.0000006})
		double calibrationParam;

/*		@Parameter({1,3,5,7,9,11,13,15,17,19})
		double restrictionDay;*/
	}

	public static void main(String[] args) {
		String[] args2 = {
				RunParallel.OPTION_SETUP, MunichCalibration.class.getName(),
				RunParallel.OPTION_PARAMS, Params.class.getName(),
				RunParallel.OPTION_TASKS, Integer.toString(1), // Set to 1 to run sequentially
				RunParallel.OPTION_ITERATIONS, Integer.toString(360),
				RunParallel.OPTION_METADATA
		};

		RunParallel.main(args2);
	}
}


