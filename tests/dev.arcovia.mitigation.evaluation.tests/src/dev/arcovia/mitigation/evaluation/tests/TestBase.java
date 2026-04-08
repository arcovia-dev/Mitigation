package dev.arcovia.mitigation.evaluation.tests;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.sat.MitigationApproach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;



public abstract class TestBase {
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
	

	protected abstract MitigationApproach getApproach(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints);
	
	protected abstract String getApproachName();
	
	static Stream<Arguments> tuhhModelProvider() {
	    return TuhhModels.getTuhhModels()
	            .entrySet()
	            .stream()
	            .flatMap(entity -> entity.getValue().stream()
	                    .map(model -> Arguments.of(entity.getKey(), model)));
	}
	
	@ParameterizedTest
	@MethodSource("tuhhModelProvider")
	void evaluateEffectiveness(String model, int variant) throws Exception {
		Thread.sleep(2);
		String name = model + "_" + variant;
		
		var dfd = loadDFD(model, name);
		
		int violationsBefore = determineViolations(dfd, analysisConstraints);
		
		MitigationApproach approach = getApproach(dfd, analysisConstraints);
		
		var repairedDFD = approach.repair(); 
		
		int violationsAfter = determineViolations(repairedDFD, analysisConstraints);
		
		assertEquals(violationsAfter, 0);
		
	    ObjectMapper mapper = new ObjectMapper();
	    Path out = Path.of("results/violation_results.json");

	    List<ViolationResult> existing = Files.exists(out)
	            ? mapper.readValue(out.toFile(), new TypeReference<List<ViolationResult>>() {})
	            : new ArrayList<>();

	    existing.add(new ViolationResult(getApproachName(),name, violationsBefore, violationsAfter));
	    mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), existing);
	}
	
	
	
	private int determineViolations(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints) {
		var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
		var analysis = new DFDDataFlowAnalysisBuilder().standalone().useCustomResourceProvider(resourceProvider)
				.build();

		analysis.initializeAnalysis();
		var flowGraph = analysis.findFlowGraphs();
		flowGraph.evaluate();

		int violations = 0;

		for (var constraint : constraints) {
			var result = constraint.findViolations(flowGraph);

			violations += result.size();

		}
		return violations;
	}
	
	public record ViolationResult(
			String Approach,
	        String modelName,
	        int violationsBefore,
	        int violationsAfter
	) {}

	private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws Exception {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("scenarios", "dfd", "TUHH-Models")
                .toString();
        return new DataFlowDiagramAndDictionary(PROJECT_NAME, Paths.get(location, model, (name + ".dataflowdiagram"))
                .toString(),
                Paths.get(location, model, (name + ".datadictionary"))
                        .toString(),
                Activator.class);
    }
}
