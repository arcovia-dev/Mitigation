package dev.abunai.confidentiality.mitigation.ranking;

import java.util.*;

public class MitigationListSimplifier {

	public static List<List<String>> simplifyMitigationList(List<List<String>> models) {
		var changeHappened = false;
		Set<Integer> indiciesNotToAdd = new HashSet<>();
		List<List<String>> newModels = new ArrayList<>();
		for (int i = 0; i < models.size(); i++) {
			for (int j = 0; j < models.size(); j++) {
				if (j <= i) {
					continue;
				}
				int differingIndex = differByOneElement(models.get(i), models.get(j));
				if (differingIndex != -1) {
					changeHappened = true;
					
					indiciesNotToAdd.add(i);
					indiciesNotToAdd.add(j);
					
					var newChosenScenarios = new ArrayList<String>(models.get(i));
					newChosenScenarios.set(differingIndex, "U");
					newModels.add(newChosenScenarios);
				}

			}
		}
		
		for(int i = 0; i < models.size();i++) {
			if(!indiciesNotToAdd.contains(i)) {
				newModels.add(models.get(i));
			}
		}
		
		newModels = newModels.stream().distinct().toList();
		
		if(changeHappened) {
			return simplifyMitigationList(newModels);
		}

		return newModels;
	}

	private static int differByOneElement(List<String> mitigationModel1, List<String> mitigationModel2) {
		var differentScenarioCount = 0;
		var differentScenarioLastIndex = 0;
		for (int i = 0; i < mitigationModel1.size(); i++) {
			if(mitigationModel1.get(i).equals("U")
					|| mitigationModel2.get(i).equals("U")) {
				continue;
			}
			if(!mitigationModel1.get(i).equals(mitigationModel2.get(i))) {
				differentScenarioCount++;
				differentScenarioLastIndex = i;
			}
		}
		var result = differentScenarioCount == 1 ? differentScenarioLastIndex : -1;
		return result;
	}

}
