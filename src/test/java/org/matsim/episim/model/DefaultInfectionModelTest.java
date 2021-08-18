package org.matsim.episim.model;

import org.assertj.core.data.Offset;
import org.junit.Test;
import org.matsim.episim.EpisimPerson;
import org.matsim.episim.EpisimTestUtils;
import org.matsim.episim.VaccinationConfigGroup;
import org.matsim.episim.VirusStrainConfigGroup;

import static org.assertj.core.api.Assertions.assertThat;


public class DefaultInfectionModelTest {

	@Test
	public void getVaccinationEffectiveness() {

		VaccinationConfigGroup vacConfig = new VaccinationConfigGroup();
		vacConfig.getParams(VaccinationType.generic)
				.setDaysBeforeFullEffect(42)
				.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.SARS_CoV_2)
						.atDay(4, 0)
						.atDay(5, 0.45)
						.atFullEffect(0.9))
				.setBoostEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.SARS_CoV_2)
						.atDay(5, 0)
						.atFullEffect(0.9))
				.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.B1351)
						.atDay(4, 0)
						.atDay(5, 0.1)
						.atFullEffect(0.2))
				.setBoostEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.B1351)
						.atDay(5, 0.2)
						.atFullEffect(0.9));

		VirusStrainConfigGroup strainConfig = new VirusStrainConfigGroup();
		VirusStrainConfigGroup.StrainParams cov2 = strainConfig.getOrAddParams(VirusStrain.SARS_CoV_2);
		VirusStrainConfigGroup.StrainParams escape = strainConfig.getOrAddParams(VirusStrain.B1351);


		EpisimPerson p = EpisimTestUtils.createPerson(true, -1);

		p.setVaccinationStatus(EpisimPerson.VaccinationStatus.yes, VaccinationType.generic, 0);

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(cov2, p, vacConfig, 0)
		).isEqualTo(1.0);

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(cov2, p, vacConfig, 3)
		).isEqualTo(1.0);

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(cov2, p, vacConfig, 5)
		).isEqualTo(1 - 0.45);

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(escape, p, vacConfig, 5)
		).isEqualTo(1.0 - 0.2 / 2);

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(cov2, p, vacConfig, 30)
		).isCloseTo(0.246, Offset.offset(0.001));

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(cov2, p, vacConfig, 42)
		).isCloseTo(0.1, Offset.offset(0.001));

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(escape, p, vacConfig, 42)
		).isCloseTo(0.8, Offset.offset(0.001));


		// test with re vaccination

		p.setReVaccinationStatus(EpisimPerson.VaccinationStatus.yes, 0);

		// person has full effect from first vaccine
		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(cov2, p, vacConfig, 1)
		).isCloseTo(0.1, Offset.offset(0.001));

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(escape, p, vacConfig, 1)
		).isEqualTo(0.8);

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(cov2, p, vacConfig, 5)
		).isCloseTo(0.1, Offset.offset(0.001));

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(escape, p, vacConfig, 5)
		).isEqualTo(0.8);

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(escape, p, vacConfig, 10)
		).isCloseTo(0.705, Offset.offset(0.01));

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(cov2, p, vacConfig, 42)
		).isCloseTo(0.1, Offset.offset(0.001));

		assertThat(
				DefaultInfectionModel.getVaccinationEffectiveness(escape, p, vacConfig, 42)
		).isCloseTo(0.1, Offset.offset(0.001));

	}
}
