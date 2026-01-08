package dev.arcovia.mitigation.ilp.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.EvaluationFunction;
import dev.arcovia.mitigation.ilp.MitigationStrategy;
import dev.arcovia.mitigation.ilp.MitigationType;
import dev.arcovia.mitigation.ilp.Node;

public class ExampleModelTest {
    private final String Model = "models/examplemodel.json";
    
    private final Constraint localLogging = new Constraint(new ConstraintDSL().ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .withoutCharacteristic("Stereotype", "local_logging")
            .create());
    
    private final Constraint logSanitization = new Constraint(new ConstraintDSL().ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "local_logging")
            .withoutCharacteristic("Stereotype", "log_sanitization")
            .create());
    
    private final Constraint loggingServer = new Constraint(List.of(new MitigationStrategy(List.of(new NodeLabel(new Label("Stereotype", "logging_server"))), 1, MitigationType.AddSink)));
    
    private final EvaluationFunction evalLoggingServer = new EvaluationFunction() {            
        @Override
        public Set<Node> evaluate(DFDFlowGraphCollection flowGraph) {
            Set<Node> violatingNodes = new HashSet<>();
            
            for (var transposeFlowGraph: flowGraph.getTransposeFlowGraphs()) {
                for (var node : transposeFlowGraph.getVertices()) {
                    if (!hasNodeCharacteristic(node, "Stereotype", "local_logging")) {
                        continue;
                    }
                    
                    if (transposeFlowGraph.stream()
                            .anyMatch(vertex -> hasNodeCharacteristic(vertex, "Stereotype", "logging_server"))) {
                        continue;
                    }
                    
                    if (!checkAcrossTFGs(flowGraph.getTransposeFlowGraphs(), node, "Stereotype", "logging_server")) {
                        var vertex = (DFDVertex) node;

                        violatingNodes.add(new Node(vertex, transposeFlowGraph, loggingServer));
                    }
                    
                    
                }
            }
            
            
            return violatingNodes;
        }

        @Override
        public boolean isMatched(DFDVertex node) {
            return false;
        }
    };
    
    
    private final Constraint authServer = new Constraint(List.of(new MitigationStrategy(List.of(new NodeLabel(new Label("Stereotype", "auth_server"))), 1, MitigationType.AddNode)));
    
    private final EvaluationFunction evalauthServer = new EvaluationFunction() {            
        @Override
        public Set<Node> evaluate(DFDFlowGraphCollection flowGraph) {
            Set<Node> violatingNodes = new HashSet<>();
            
            for (var transposeFlowGraph: flowGraph.getTransposeFlowGraphs()) {
                for (var node : transposeFlowGraph.getVertices()) {
                    if (!hasNodeCharacteristic(node, "Stereotype", "internal") || !hasIncomingCharacteristic(node,"Stereotype", "entrypoint" )) {
                        continue;
                    }
                    
                    if (transposeFlowGraph.stream()
                            .anyMatch(vertex -> hasNodeCharacteristic(vertex, "Stereotype", "auth_server"))) {
                        continue;
                    }
                    
                    if (!checkAcrossTFGs(flowGraph.getTransposeFlowGraphs(), node, "Stereotype", "auth_server")) {
                        var vertex = (DFDVertex) node;

                        violatingNodes.add(new Node(vertex, transposeFlowGraph, authServer));
                    }
                    
                    
                }
            }
            
            
            return violatingNodes;
        }

        @Override
        public boolean isMatched(DFDVertex node) {
            return false;
        }
    };
    
    private final Constraint authenticated_request = new Constraint(new ConstraintDSL().ofData()
            .withoutLabel("Stereotype", "authenticated_request")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create());
    
    private final Constraint personalLogging = new Constraint(new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Logging", "Server")
            .create());
    
    private final Constraint personalStorageNonEu = new Constraint(new ConstraintDSL().ofData()
            .withLabel("Sensitivity", "Personal")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Location", "nonEU")
            .create());
    
    private final Constraint adminStorage = new Constraint(new ConstraintDSL().ofData()
            .withLabel("User", "admin")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "Storage")
            .create(), List.of(new MitigationStrategy(List.of(new OutgoingDataLabel(new Label("User","admin"))), 1, MitigationType.DeleteFlow)));
    
    private final Constraint adminEncryption = new Constraint(new ConstraintDSL().ofData()
            .withLabel("User", "admin")
            .withoutLabel("Encryption", "encrypted")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create());
    
    private final List<Constraint> Constraints = List.of(localLogging, logSanitization, loggingServer, authServer, authenticated_request,
            personalLogging, personalStorageNonEu, adminStorage, adminEncryption);
    
    
    public void setup() {
        loggingServer.addEvalFunction(evalLoggingServer);
        loggingServer.addPrecondition(new NodeLabel(new Label("Stereotype", "local_logging")));
        authServer.addEvalFunction(evalauthServer);
    }
    
    
    @Test
    public void StandardTest(){
        setup();
        var optimization = new OptimizationManager(Model, Constraints, false);

        var result = optimization.repair();
        
        result.save("models/", "examplemodel-Standardrepair");
        
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result)
                .save("models/", "examplemodel-Standardrepair.json");
        
        assertTrue(optimization.isViolationFree(result));
        
        
    }
    
    private boolean hasNodeCharacteristic(AbstractVertex<?> node, String type, String value) {
        return node.getAllVertexCharacteristics()
                .stream()
                .anyMatch(n -> n.getTypeName()
                        .equals(type)
                        && n.getValueName()
                                .equals(value));
    }
    
    private boolean hasIncomingCharacteristic(AbstractVertex<?> node, String type, String value) {
        return node.getAllIncomingDataCharacteristics()
                .stream()
                .anyMatch(v -> v.getAllCharacteristics()
                        .stream()
                        .anyMatch(c -> c.getTypeName()
                                .equals(type)
                                && c.getValueName()
                                        .equals(value)));
    }
    
    private boolean checkAcrossTFGs(List<? extends AbstractTransposeFlowGraph> transposeFlowGraphs, AbstractVertex<?> node, String Type, String value) {
        for (var transposeFlowGraph : transposeFlowGraphs) {
            if (transposeFlowGraph.getVertices().stream().anyMatch(n -> n.getReferencedElement().equals(node.getReferencedElement()))) {
                if (transposeFlowGraph.stream()
                        .anyMatch(vertex -> hasNodeCharacteristic(vertex, "Stereotype", "logging_server"))) {
                    return true;
                }
            }
        }
        
        return false;
    }

}
