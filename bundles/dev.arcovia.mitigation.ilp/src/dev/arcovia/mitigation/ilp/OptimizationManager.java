package dev.arcovia.mitigation.ilp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.result.DSLResult;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.web2dfd.Web2DFDConverter;
import org.dataflowanalysis.converter.web2dfd.WebEditorConverterModel;


public class OptimizationManager {
    private final DataFlowDiagramAndDictionary dfd;
    
    
    private final Logger logger = Logger.getLogger(OptimizationManager.class);
    
    private final List<Constraint> constraints;
     
    private Set<Node> violatingNodes = new HashSet<>(); 
    
    private List<List<Mitigation>> mitigations = new ArrayList<>();
    private Set<Mitigation> allMitigations = new HashSet<>();
    
    public OptimizationManager(String dfdLocation, List<Constraint> constraints) {
        this.dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));
        this.constraints = constraints;        
    }
    
    public DataFlowDiagramAndDictionary repair() throws IOException {
        analyseDFD();
        
        for (var node : violatingNodes) {            
            addMitigations(node.getpossibleMitigations());
        }
        
        var solver = new ILPSolver();
        solver.writeLP(mitigations, allMitigations);
        
        return dfd;
    }
    
    private void addMitigations(List<Mitigation> mitigation) {
        //done to prevent having the same Mitigation twice by replacing duplicates with the original/first appearance
        List<Mitigation> merged = mitigation.stream()
                .map(u -> allMitigations.stream()
                        .filter(u::equals)
                        .findFirst()
                        .orElse(u))
                        .collect(Collectors.toList());
        
        mitigations.add(merged);
        allMitigations.addAll(merged);
    }
    
    private List<Constraint> getConstraints(List<AnalysisConstraint> constraints) {
        List<Constraint> constraintList = new ArrayList<>();
        
        for(var constraint : constraints)
            constraintList.add(new Constraint(constraint));
        
        return constraintList;
    }
    
    private void analyseDFD() {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        
        for(var constraint : constraints) {
            List<DSLResult> results = constraint.dsl.findViolations(flowGraph);
            for( var result : results) {
                var tfg = result.getTransposeFlowGraph();
                for (var vertex : result.getMatchedVertices())
                    violatingNodes.add(new Node((DFDVertex) vertex, tfg, constraint));
            }
        }        
    }
}
