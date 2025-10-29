package dev.arcovia.mitigation.ilp.tests;

import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.ilp.OptimizationManager;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.ModelCostCalculator;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;

public class TUHH_Test {
	final AnalysisConstraint entryViaGatewayOnly = new ConstraintDSL().ofData().withLabel("Stereotype", "entrypoint")
			.withoutLabel("Stereotype", "gateway").neverFlows().toVertex().withCharacteristic("Stereotype", "internal")
			.create();
	final AnalysisConstraint nonInternalGateway = new ConstraintDSL().ofData().neverFlows().toVertex()
			.withCharacteristic("Stereotype", "gateway").withCharacteristic("Stereotype", "internal").create();
	final AnalysisConstraint authenticatedRequest = new ConstraintDSL().ofData()
			.withoutLabel("Stereotype", "authenticated_request").neverFlows().toVertex()
			.withCharacteristic("Stereotype", "internal").create();
	final AnalysisConstraint transformedEntry = new ConstraintDSL().ofData().withLabel("Stereotype", "entrypoint")
			.withoutLabel("Stereotype", "transform_identity_representation").neverFlows().toVertex()
			.withCharacteristic("Stereotype", "internal").create();
	final AnalysisConstraint tokenValidation = new ConstraintDSL().ofData().withLabel("Stereotype", "entrypoint")
			.withoutLabel("Stereotype", "token_validation").neverFlows().toVertex()
			.withCharacteristic("Stereotype", "internal").create();
	final AnalysisConstraint loginAttempts = new ConstraintDSL().ofData().neverFlows().toVertex()
			.withCharacteristic("Stereotype", "authorization_server")
			.withoutCharacteristic("Stereotype", "login_attempts_regulation").create();
	final AnalysisConstraint encryptedEntry = new ConstraintDSL().ofData().withLabel("Stereotype", "entrypoint")
			.withoutLabel("Stereotype", "encrypted_connection").neverFlows().toVertex().create();
	final AnalysisConstraint encryptedInternals = new ConstraintDSL().ofData().withLabel("Stereotype", "internal")
			.withoutLabel("Stereotype", "encrypted_connection").neverFlows().toVertex().create();
	final AnalysisConstraint localLogging = new ConstraintDSL().ofData().neverFlows().toVertex()
			.withCharacteristic("Stereotype", "internal").withoutCharacteristic("Stereotype", "local_logging").create();
	final AnalysisConstraint logSanitization = new ConstraintDSL().ofData().neverFlows().toVertex()
			.withCharacteristic("Stereotype", "local_logging").withoutCharacteristic("Stereotype", "log_sanitization")
			.create();

	final List<AnalysisConstraint> analysisConstraints = List.of(entryViaGatewayOnly, nonInternalGateway,
			authenticatedRequest, transformedEntry, tokenValidation, loginAttempts, encryptedEntry, encryptedInternals,
			localLogging, logSanitization);

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
				dfdConverter.convert(result).save("models/", "temp-repaired.json");

				assertTrue(optimization.isViolationFree(result, analysisConstraints));
			}
		}
		System.out.println(scalabilityValues);
	}

	final Map<Label, Integer> minCosts = Map.ofEntries(entry(new Label("Stereotype", "gateway"), 1),
			entry(new Label("Stereotype", "authenticated_request"), 1),
			entry(new Label("Stereotype", "transform_identity_representation"), 1),
			entry(new Label("Stereotype", "token_validation"), 1),
			entry(new Label("Stereotype", "login_attempts_regulation"), 1),
			entry(new Label("Stereotype", "encrypted_connection"), 1),
			entry(new Label("Stereotype", "log_sanitization"), 1), entry(new Label("Stereotype", "local_logging"), 1));

	@Test
	public void efficiencyTest() throws StandaloneInitializationException {
		var tuhhModels = TuhhModels.getTuhhModels();
		List<String> modelRepairMoreExpensive = new ArrayList<>();

		for (var model : tuhhModels.keySet()) {
			if (!tuhhModels.get(model).contains(0))
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
				dfdConverter.convert(repairedDfd).save("efficencyTest/", model + "_" + variant + "-repaired.json");

				List<dev.arcovia.mitigation.sat.Constraint> satConstraint = new ArrayList<>();
				for (var cons : constraint) {
					var translation = new CNFTranslation(cons);
					dev.arcovia.mitigation.sat.Constraint c = translation.constructCNF().get(0);
					satConstraint.add(c);
				}

				var ilpCost = new ModelCostCalculator(repairedDfd, satConstraint, minCosts)
						.calculateCostWithoutForwarding();
				var numactions = optimization.getCost();

				if (numactions < ilpCost) {
					System.out.println(variant + " Actions:" + numactions + " Cost: " + ilpCost);
				}

				var tuhhCost = new ModelCostCalculator(loadDFD(model, model + "_" + variant), satConstraint, minCosts)
						.calculateCostWithoutForwarding();

				System.out.println(ilpCost + " <= " + tuhhCost + " : " + (ilpCost <= tuhhCost));
				if (ilpCost > tuhhCost) {
					modelRepairMoreExpensive.add(model + "_" + variant);
				}
			}
		}
		System.out.println(modelRepairMoreExpensive);

	}

	@Disabled
	@Test
	public void runSpecific() throws StandaloneInitializationException {
		String model = "anilallewar";
		int variant = 0;
		String name = model + "_" + variant;

		DataFlowDiagramAndDictionary dfd = loadDFD(model, name);

		var optimization = new OptimizationManager(dfd,
				List.of(encryptedEntry, entryViaGatewayOnly, nonInternalGateway));

		var result = optimization.repair();

		List<dev.arcovia.mitigation.sat.Constraint> satConstraint = new ArrayList<>();
		for (var cons : List.of(encryptedEntry, entryViaGatewayOnly, nonInternalGateway)) {
			var translation = new CNFTranslation(cons);
			dev.arcovia.mitigation.sat.Constraint c = translation.constructCNF().get(0);
			satConstraint.add(c);
		}

		var dfdConverter = new DFD2WebConverter();
		dfdConverter.convert(result).save("models/", "temp-repaired.json");

		assertTrue(optimization.isViolationFree(result, analysisConstraints));
	}

	private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
		final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
		final String location = Paths.get("scenarios", "dfd", "TUHH-Models").toString();
		var dfd = new DataFlowDiagramAndDictionary(PROJECT_NAME,
				Paths.get(location, model, (name + ".dataflowdiagram")).toString(),
				Paths.get(location, model, (name + ".datadictionary")).toString(), Activator.class);
		return dfd;
	}
}
