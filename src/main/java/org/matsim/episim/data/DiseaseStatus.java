package org.matsim.episim.data;

/**
 * Disease status of a person.
 */
public enum DiseaseStatus {
	susceptible, infectedButNotContagious, contagious, showingSymptoms,
	seriouslySick, critical, seriouslySickAfterCritical, recovered
}
