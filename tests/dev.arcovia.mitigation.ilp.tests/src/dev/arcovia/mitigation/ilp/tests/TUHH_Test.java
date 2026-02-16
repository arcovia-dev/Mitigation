package dev.arcovia.mitigation.ilp.tests;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.core.AbstractVertex;

import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.ilp.Constraint;
import dev.arcovia.mitigation.ilp.EvaluationFunction;
import dev.arcovia.mitigation.ilp.MitigationStrategy;
import dev.arcovia.mitigation.ilp.MitigationType;
import dev.arcovia.mitigation.ilp.Node;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.ModelCostCalculator;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.OutgoingDataLabel;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class TUHH_Test {
    final AnalysisConstraint entryViaGatewayOnly = new ConstraintDSL().ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "gateway")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create();
    final AnalysisConstraint nonInternalGateway = new ConstraintDSL().ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "gateway")
            .withCharacteristic("Stereotype", "internal")
            .create();
    final AnalysisConstraint authenticatedRequest = new ConstraintDSL().ofData()
            .withoutLabel("Stereotype", "authenticated_request")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create();
    final AnalysisConstraint transformedEntry = new ConstraintDSL().ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "transform_identity_representation")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create();
    final AnalysisConstraint tokenValidation = new ConstraintDSL().ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "token_validation")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create();
    final AnalysisConstraint loginAttempts = new ConstraintDSL().ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "authorization_server")
            .withoutCharacteristic("Stereotype", "login_attempts_regulation")
            .create();
    final AnalysisConstraint encryptedEntry = new ConstraintDSL().ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "encrypted_connection")
            .neverFlows()
            .toVertex()
            .create();
    final AnalysisConstraint encryptedInternals = new ConstraintDSL().ofData()
            .withLabel("Stereotype", "internal")
            .withoutLabel("Stereotype", "encrypted_connection")
            .neverFlows()
            .toVertex()
            .create();
    final AnalysisConstraint localLogging = new ConstraintDSL().ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .withoutCharacteristic("Stereotype", "local_logging")
            .create();
    final AnalysisConstraint logSanitization = new ConstraintDSL().ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "local_logging")
            .withoutCharacteristic("Stereotype", "log_sanitization")
            .create();

    final List<AnalysisConstraint> analysisConstraints = List.of(entryViaGatewayOnly, nonInternalGateway, authenticatedRequest, transformedEntry,
            tokenValidation, loginAttempts, encryptedEntry, encryptedInternals, localLogging, logSanitization);

    @Test
    public void main() throws StandaloneInitializationException {
        var tuhhModels = TuhhModels.getTuhhModels();
        List<Long> scalabilityValues = new ArrayList<>();

        for (var model : tuhhModels.keySet()) {
            for (int variant : tuhhModels.get(model)) {
                String name = model + "_" + variant;

                System.out.println(name);

                DataFlowDiagramAndDictionary dfd = loadDFD(model, name);

                var optimization = new OptimizationManager(dfd, analysisConstraints);

                long startTime = System.currentTimeMillis();
                var result = optimization.repair();
                long endTime = System.currentTimeMillis();

                scalabilityValues.add(endTime - startTime);

                var dfdConverter = new DFD2WebConverter();
                dfdConverter.convert(result)
                        .save("models/", "temp-repaired.json");

                assertTrue(optimization.isViolationFree(result));
            }
        }
        System.out.println(scalabilityValues);
    }

    final Map<Label, Integer> minCosts = Map.ofEntries(entry(new Label("Stereotype", "gateway"), 1),
            entry(new Label("Stereotype", "authenticated_request"), 1), entry(new Label("Stereotype", "transform_identity_representation"), 1),
            entry(new Label("Stereotype", "token_validation"), 1), entry(new Label("Stereotype", "login_attempts_regulation"), 1),
            entry(new Label("Stereotype", "encrypted_connection"), 1), entry(new Label("Stereotype", "log_sanitization"), 1),
            entry(new Label("Stereotype", "local_logging"), 1));

    @Test
    public void efficiencyTest() throws StandaloneInitializationException {
        var tuhhModels = TuhhModels.getTuhhModels();
        List<String> modelRepairMoreExpensive = new ArrayList<>();

        Map<String, Integer> tuhhCosts = new LinkedHashMap<>();
        Map<String, Integer> ilpCosts = new LinkedHashMap<>();

        for (var model : tuhhModels.keySet()) {
            if (!tuhhModels.get(model)
                    .contains(0))
                continue;

            System.out.println("Checking " + model);

            for (int variant : tuhhModels.get(model)) {
                List<AnalysisConstraint> constraint = switch (variant) {
                    case 1 -> List.of(entryViaGatewayOnly, nonInternalGateway);
                    case 2 -> List.of(authenticatedRequest);
                    case 4 -> List.of(transformedEntry);
                    case 5 -> List.of(tokenValidation);
                    case 7 -> List.of(encryptedEntry, entryViaGatewayOnly, nonInternalGateway);
                    case 8 -> List.of(encryptedInternals);
                    case 10 -> List.of(localLogging);
                    case 11 -> List.of(localLogging, logSanitization);
                    default -> null;
                };
                if (constraint == null)
                    continue;

                DataFlowDiagramAndDictionary dfd = loadDFD(model, model + "_0");
                System.out.println("Comparing to " + model + "_" + variant);

                var optimization = new OptimizationManager(dfd, constraint);

                var repairedDfd = optimization.repair();

                var dfdConverter = new DFD2WebConverter();
                dfdConverter.convert(repairedDfd)
                        .save("efficencyTest/", model + "_" + variant + "-repaired.json");

                List<dev.arcovia.mitigation.sat.Constraint> satConstraint = new ArrayList<>();
                for (var cons : constraint) {
                    var translation = new CNFTranslation(cons);
                    dev.arcovia.mitigation.sat.Constraint c = translation.constructCNF()
                            .get(0);
                    satConstraint.add(c);
                }

                var ilpCost = new ModelCostCalculator(repairedDfd, satConstraint, minCosts).calculateCostWithoutForwarding();

                var tuhhCost = new ModelCostCalculator(loadDFD(model, model + "_" + variant), satConstraint, minCosts)
                        .calculateCostWithoutForwarding();

                System.out.println(ilpCost + " <= " + tuhhCost + " : " + (ilpCost <= tuhhCost));
                if (ilpCost > tuhhCost) {
                    modelRepairMoreExpensive.add(model + "_" + variant);
                }
                ilpCosts.put(model + "_" + variant, ilpCost);
                tuhhCosts.put(model + "_" + variant, tuhhCost);

            }
        }
        System.out.println(modelRepairMoreExpensive);

        System.out.println(ilpCosts);
        System.out.println(tuhhCosts);
    }
    

    @Test
    public void runCompleteTUHH() throws StandaloneInitializationException {
        var tuhhModels = TuhhModels.getTuhhModels();
        
        Map<String, Integer> amountViolations = new HashMap<>();       
        
        
        for (var model : tuhhModels.keySet()) {

            for (int variant : tuhhModels.get(model)) {
                final Constraint loggingServer = new Constraint(List.of(new MitigationStrategy(List.of(new NodeLabel(new Label("Stereotype", "logging_server"))), 1, MitigationType.AddSink)));
                
                final EvaluationFunction evalLoggingServer = new EvaluationFunction() {            
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
                
                final MitigationStrategy authServerMit= new MitigationStrategy(List.of(new NodeLabel(new Label("Stereotype", "authorization_server")), new NodeLabel(new Label("Stereotype", "login_attempts_regulation")), new OutgoingDataLabel(new Label("Stereotype", "transform_identity_representation"))), 1, MitigationType.AddNode);
                
                final Constraint authServer = new Constraint(List.of(authServerMit));
                
                final EvaluationFunction evalauthServer = new EvaluationFunction() {            
                    @Override
                    public Set<Node> evaluate(DFDFlowGraphCollection flowGraph) {
                        Set<Node> violatingNodes = new HashSet<>();
                        
                        for (var transposeFlowGraph: flowGraph.getTransposeFlowGraphs()) {
                            for (var node : transposeFlowGraph.getVertices()) {
                                if (!hasNodeCharacteristic(node, "Stereotype", "internal") || !hasIncomingCharacteristic(node,"Stereotype", "entrypoint" )) {
                                    continue;
                                }
                                
                                if (transposeFlowGraph.stream()
                                        .anyMatch(vertex -> hasNodeCharacteristic(vertex, "Stereotype", "authorization_server"))) {
                                    continue;
                                }
                                
                                if (!checkAcrossTFGs(flowGraph.getTransposeFlowGraphs(), node, "Stereotype", "authorization_server")) {
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
                
                List<Constraint> constraints = new ArrayList<>();
                
                loggingServer.addEvalFunction(evalLoggingServer);
                loggingServer.addPrecondition(new NodeLabel(new Label("Stereotype", "local_logging")));
                authServer.addEvalFunction(evalauthServer);
                
                for (var constraint : analysisConstraints) {
                    constraints.add(new Constraint(constraint));
                }
                
                constraints.add(authServer);
                constraints.add(loggingServer);
                
                
                
                String name = model + "_" + variant;

                System.out.println(name);
                
                DataFlowDiagramAndDictionary dfd = loadDFD(model, name);

                var optimization = new OptimizationManager(dfd, constraints, false);
                
                amountViolations.put(name, optimization.amountOfViolations());

                var repairedDfd = optimization.repair();
                
                repairedDfd.save("models/", "temp-repaired");
                
                assertTrue(optimization.isViolationFree(repairedDfd));
            }
        }
        
        
        Map<String, Integer> amountViolationsConstraint = new HashMap<>();
        
        for (var model : tuhhModels.keySet()) {
            if (!tuhhModels.get(model)
                    .contains(0))
                continue;

            System.out.println("Checking " + model);
            
            final Constraint loggingServer = new Constraint(List.of(new MitigationStrategy(List.of(new NodeLabel(new Label("Stereotype", "logging_server"))), 1, MitigationType.AddSink)));
            
            final EvaluationFunction evalLoggingServer = new EvaluationFunction() {            
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
            
            final MitigationStrategy authServerMit= new MitigationStrategy(List.of(new NodeLabel(new Label("Stereotype", "authorization_server")), new NodeLabel(new Label("Stereotype", "login_attempts_regulation")), new OutgoingDataLabel(new Label("Stereotype", "transform_identity_representation"))), 1, MitigationType.AddNode);
            
            final Constraint authServer = new Constraint(List.of(authServerMit));
            
            final EvaluationFunction evalauthServer = new EvaluationFunction() {            
                @Override
                public Set<Node> evaluate(DFDFlowGraphCollection flowGraph) {
                    Set<Node> violatingNodes = new HashSet<>();
                    
                    for (var transposeFlowGraph: flowGraph.getTransposeFlowGraphs()) {
                        for (var node : transposeFlowGraph.getVertices()) {
                            if (!hasNodeCharacteristic(node, "Stereotype", "internal") || !hasIncomingCharacteristic(node,"Stereotype", "entrypoint" )) {
                                continue;
                            }
                            
                            if (transposeFlowGraph.stream()
                                    .anyMatch(vertex -> hasNodeCharacteristic(vertex, "Stereotype", "authorization_server"))) {
                                continue;
                            }
                            
                            if (!checkAcrossTFGs(flowGraph.getTransposeFlowGraphs(), node, "Stereotype", "authorization_server")) {
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
                        
            loggingServer.addEvalFunction(evalLoggingServer);
            loggingServer.addPrecondition(new NodeLabel(new Label("Stereotype", "local_logging")));
            authServer.addEvalFunction(evalauthServer);
            
            
            for (int variant : tuhhModels.get(model)) {
                List<Constraint> constraint = switch (variant) {
                    case 1 -> List.of(new Constraint(entryViaGatewayOnly), new Constraint(nonInternalGateway));
                    case 2 -> List.of(new Constraint(authenticatedRequest));
                    case 4 -> List.of(new Constraint(transformedEntry));
                    case 5 -> List.of(new Constraint(tokenValidation));
                    case 6 -> List.of(authServer);
                    case 7 -> List.of(new Constraint(encryptedEntry), new Constraint(entryViaGatewayOnly), new Constraint(nonInternalGateway));
                    case 8 -> List.of(new Constraint(encryptedInternals));
                    case 10 -> List.of(new Constraint(localLogging));
                    case 11 -> List.of(new Constraint(localLogging), new Constraint(logSanitization));
                    case 12 -> List.of(new Constraint(localLogging), new Constraint(logSanitization), loggingServer);
                    default -> null;
                };
                if (constraint == null)
                    continue;

                String name = model + "_" + 0;

                System.out.println(name);
                
                DataFlowDiagramAndDictionary dfd = loadDFD(model, name);

                var optimization = new OptimizationManager(dfd, constraint, false);
                
                amountViolationsConstraint.put(model + "_" + variant, optimization.amountOfViolations());

                var repairedDfd = optimization.repair();
                
                assertTrue(optimization.isViolationFree(repairedDfd));
            }
        }
        System.out.println(amountViolations);
        System.out.println(amountViolationsConstraint);
    }
    
    @Disabled
    @Test
    public void runSpecific() throws StandaloneInitializationException {
        String model = "mudigal-technologies";
        int variant = 2;
        String name = model + "_" + variant;

        DataFlowDiagramAndDictionary dfd = loadDFD(model, name);

        var optimization = new OptimizationManager(dfd, analysisConstraints);

        var result = optimization.repair();

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(result)
                .save("models/", "temp-repaired.json");

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
