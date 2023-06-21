package org.matsim.episim.munich;

import org.matsim.episim.EpisimPerson;

import java.util.ArrayList;
import java.util.List;

public class SiloPerson {

	private int id;
	/**
	 * social network alter lists.
	 */
	private List<Integer> alterLists = new ArrayList<>();

	private List<EpisimPerson> episimPersonList = new ArrayList<>();

	public SiloPerson(int siloPersonId) {
		this.id = siloPersonId;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Integer> getAlterLists() {
		return alterLists;
	}

	public void setAlterLists(List<Integer> alterLists) {
		this.alterLists = alterLists;
	}

	public List<EpisimPerson> getEpisimPersonList() {
		return episimPersonList;
	}

	public void setEpisimPersonList(List<EpisimPerson> episimPersonList) {
		this.episimPersonList = episimPersonList;
	}
}
