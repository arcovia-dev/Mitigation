package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.EvaluationFunction;
import dev.arcovia.mitigation.ilp.MitigationStrategy;
import dev.arcovia.mitigation.ilp.MitigationType;
import dev.arcovia.mitigation.ilp.Node;
import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

public class AddNodeAndFlowsTest {
    private final Constraint customConstraint = new Constraint(List.of(new MitigationStrategy(List.of(new NodeLabel(new Label("Stereotype", "logging_server"))), 1, MitigationType.AddNode)));
    
    private final EvaluationFunction evalFunction = new EvaluationFunction() {            
        @Override
        public Set<Node> evaluate(DFDFlowGraphCollection flowGraph) {
            Set<Node> violatingNodes = new HashSet<>();
            
            for (var transposeFlowGraph: flowGraph.getTransposeFlowGraphs()) {
                if (transposeFlowGraph.stream()
                        .anyMatch(node -> hasNodeCharacteristic(node, "Stereotype", "logging_server")))
                    return violatingNodes;
            }
            
            for (var transposeFlowGraph: flowGraph.getTransposeFlowGraphs()) {
                for (var node : transposeFlowGraph.getVertices()) {
                    var vertex = (DFDVertex) node;
                    
                    if(vertex.getAllOutgoingDataCharacteristics().isEmpty()) continue;
                    
                    violatingNodes.add(new Node(vertex, transposeFlowGraph, customConstraint));
                }
            }
            
            
            return violatingNodes;
        }

        @Override
        public boolean isMatched(DFDVertex node) {
            return false;
        }
    };
    
    private boolean hasNodeCharacteristic(AbstractVertex<?> node, String type, String value) {
        return node.getAllVertexCharacteristics()
                .stream()
                .anyMatch(n -> n.getTypeName()
                        .equals(type)
                        && n.getValueName()
                                .equals(value));
    }
    
    
    @Test
    public void addNodeTest() throws StandaloneInitializationException {
        String model = "jferrater";
        int variant = 0;
        String name = model + "_" + variant;

        DataFlowDiagramAndDictionary dfd = loadDFD(model, name);
        
        customConstraint.addEvalFunction(evalFunction);
        
        var optimization = new OptimizationManager(dfd, List.of(customConstraint), false);

        var result = optimization.repair();
                
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result)
                .save("models/", "addedNode.json");
        
        assertTrue(optimization.isViolationFree(result));
        
    }
    
    
    private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("scenarios", "dfd", "TUHH-Models")
                .toString();
        var dfd = new DataFlowDiagramAndDictionary(PROJECT_NAME, Paths.get(location, model, (name + ".dataflowdiagram"))
                .toString(),
                Paths.get(location, model, (name + ".datadictionary"))
                        .toString(),
                Activator.class);
        return dfd;
    }
}
