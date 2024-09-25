package dev.abunai.confidentiality.mitigation.tests;

import java.util.List;

/*
 * Class for the Calculation of P@K and MAP@K for a correct and a given ranking.
 * */
public class MetricCalculator {

	public static float calculatePAtK(int k, List<String> correctRanking,
			List<String> evaluatingRanking) {;
		var correctElementsCount = evaluatingRanking.stream().limit(k)
				.filter(e -> correctRanking.contains(e))
				.count();
		float res = (float) correctElementsCount / (float) k;
		return res;
	}
	
	public static float calculateMAPAtK(int k, List<String> correctRanking,
			List<String> evaluatingRanking) {
		float precissionValueSum = 0.0f;
		
		for(int i = 1; i <= k;i++) {
			precissionValueSum += calculatePAtK(i,correctRanking,evaluatingRanking);
		}
		
		float res = precissionValueSum / (float)k;
		return res;
	}
	
	public static int determineR(List<String> correctRanking,
			List<String> evaluatingRanking) {
		var lastIndex = 0;
		
		for(int i = 0; i < evaluatingRanking.size();i++) {
			if (correctRanking.contains(evaluatingRanking.get(i))) {
				lastIndex = i;
			}
		}
		
		return lastIndex + 1;
	}
	
}
