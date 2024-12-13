package dev.arcovia.mitigation.ranking;

import java.util.List;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;

public class UncertaintySubset {
	
	private final List<UncertaintySource> allSources;
	private final List<UncertaintySource> subsetSources;
	
	public UncertaintySubset(List<UncertaintySource> allSources, List<UncertaintySource> subsetSources) {
		this.subsetSources = subsetSources;
		this.allSources = allSources;
	}
	
	public List<UncertaintySource> getNotInSubsetSources(){
		return allSources.stream().filter(s -> !subsetSources.contains(s)).toList();
	}

	public List<UncertaintySource> getSubsetSources() {
		return subsetSources;
	}

	public List<UncertaintySource> getAllSources() {
		return allSources;
	}

}
