package dev.arcovia.mitigation.ilp;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;

import dev.arcovia.mitigation.sat.CompositeLabel;
import dev.arcovia.mitigation.sat.LabelCategory;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;

import java.util.List;
import java.util.ArrayList;

public class Constraint {
	public final AnalysisConstraint dsl;
	private final List<MitigationStrategy> mitigations;

	public Constraint(AnalysisConstraint dsl, List<MitigationStrategy> mitigations) {
		this.dsl = dsl;
		this.mitigations = mitigations;
	}

	public Constraint(AnalysisConstraint dsl) {
		this.dsl = dsl;
		this.mitigations = determineMitigations();

	}

	public List<MitigationStrategy> getMitigations() {
		return mitigations;
	}

	public boolean isPrecondition(CompositeLabel label) {
		var translation = new CNFTranslation(dsl);
		var literals = translation.constructCNF().get(0).literals();
		
		for (var lit : literals) {
			if (!lit.positive() && lit.compositeLabel().equals(label))
				return true;
		}
		return false;
	}

	private List<MitigationStrategy> determineMitigations() {
		var translation = new CNFTranslation(dsl);
		
		if(translation.constructCNF().size() > 1 ) {
		    System.out.println("this constraint is not supportet - yet");
		}
		    
		
		var literals = translation.constructCNF().get(0).literals();

		List<MitigationStrategy> mitigations = new ArrayList<>();
		
		var neverFlows = dsl.getVertexDestinationSelectors().getSelectors().toString();
		
		for (var lit : literals) {
			if (lit.positive()) {
				MitigationType type;

				if (lit.compositeLabel().category() == LabelCategory.Node)
					type = MitigationType.NodeLabel;
				else
					type = MitigationType.DataLabel;

				mitigations.add(new MitigationStrategy(lit.compositeLabel(), 1, type));
			}
			else {
                var label = lit.compositeLabel().label().type() + "." + lit.compositeLabel().label().value();
                
                if (neverFlows.contains(label)) {
                    MitigationType type;

                    if (lit.compositeLabel().category() == LabelCategory.Node)
                        type = MitigationType.DeleteNodeLabel;
                    else
                        type = MitigationType.DeleteDataLabel;

                    mitigations.add(new MitigationStrategy(lit.compositeLabel(), 1000, type));
                
                }
                
            }
		}
		
		

		return mitigations;
	}

}
