package dev.arcovia.mitigation.evaluation.tests;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.web2dfd.Web2DFDConverter;
import org.dataflowanalysis.converter.web2dfd.WebEditorConverterModel;
import org.dataflowanalysis.examplemodels.Activator;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.MeasurementWriter;
import dev.arcovia.mitigation.sat.MitigationApproach;
import dev.arcovia.mitigation.sat.ModelCostCalculator;
import dev.arcovia.mitigation.sat.RunConfig;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.sat.timeMeasurement;
import dev.arcovia.mitigation.sat.Scaler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public abstract class TestBase {
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

	protected abstract MitigationApproach getApproach(DataFlowDiagramAndDictionary dfd,
			List<AnalysisConstraint> constraints);

	protected abstract String getApproachName();

	static Stream<Arguments> tuhhModelProvider() {
		return TuhhModels.getTuhhModels().entrySet().stream()
				.flatMap(entity -> entity.getValue().stream().map(model -> Arguments.of(entity.getKey(), model)));
	}

	@ParameterizedTest
	@MethodSource("tuhhModelProvider")
	void evaluateEffectiveness(String model, int variant) throws Exception {	
		String name = model + "_" + variant;

		var dfd = loadDFD(model, name);

		int violationsBefore = determineViolations(dfd, analysisConstraints);

		MitigationApproach approach = getApproach(dfd, analysisConstraints);

		var repairedDFD = approach.repair();

		int violationsAfter = determineViolations(repairedDFD, analysisConstraints);

		assertEquals(0, violationsAfter);

		ObjectMapper mapper = new ObjectMapper();
		Path out = Path.of("testresults/" + getApproachName().toLowerCase() + "_violation_results.json");

		List<ViolationResult> existing = Files.exists(out)
				? mapper.readValue(out.toFile(), new TypeReference<List<ViolationResult>>() {
				})
				: new ArrayList<>();

		existing.add(new ViolationResult(name, violationsBefore, violationsAfter));
		mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), existing);
	}

	static Stream<Arguments> tuhhModelProviderBaseModel() {
		return TuhhModels.getTuhhModels().entrySet().stream().filter(model -> model.getValue().contains(0))
				.flatMap(entity -> entity.getValue().stream().map(model -> Arguments.of(entity.getKey(), model)));
	}

	final Map<Label, Integer> minCosts = Map.ofEntries(entry(new Label("Stereotype", "gateway"), 1),
			entry(new Label("Stereotype", "authenticated_request"), 1),
			entry(new Label("Stereotype", "transform_identity_representation"), 1),
			entry(new Label("Stereotype", "token_validation"), 1),
			entry(new Label("Stereotype", "login_attempts_regulation"), 1),
			entry(new Label("Stereotype", "encrypted_connection"), 1),
			entry(new Label("Stereotype", "log_sanitization"), 1), entry(new Label("Stereotype", "local_logging"), 1));

	@ParameterizedTest
	@MethodSource("tuhhModelProviderBaseModel")
	void evaluateCost(String model, int variant) throws Exception {
		String name = model + "_" + 0;

		List<AnalysisConstraint> constraints = switch (variant) {
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
		if (constraints == null) {
			return;
		}

		var dfd = loadDFD(model, name);

		MitigationApproach approach = getApproach(dfd, constraints);

		var repairedDFD = approach.repair();

		List<dev.arcovia.mitigation.sat.Constraint> satConstraint = new ArrayList<>();
		for (var constraint : constraints) {
			var translation = new CNFTranslation(constraint);
			dev.arcovia.mitigation.sat.Constraint c = translation.constructCNF().get(0);
			satConstraint.add(c);
		}

		var approachCost = new ModelCostCalculator(repairedDFD, satConstraint, minCosts)
				.calculateCostWithoutForwarding();

		var tuhhCost = new ModelCostCalculator(loadDFD(model, model + "_" + variant), satConstraint, minCosts)
				.calculateCostWithoutForwarding();

		ObjectMapper mapper = new ObjectMapper();
		Path out = Path.of("testresults/" + getApproachName().toLowerCase() + "_efficiency_results.json");

		List<CostResult> existing = Files.exists(out)
				? mapper.readValue(out.toFile(), new TypeReference<List<CostResult>>() {
				})
				: new ArrayList<>();

		existing.add(new CostResult(model, variant, approachCost, tuhhCost));
		mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), existing);

	}

	private static final String SCALE_DFD = "models/sourceSink.json";

	private static final AnalysisConstraint scalabilityConstraint = new ConstraintDSL().ofData()
			.withLabel("Sensitivity", "Personal").withoutLabel("Encryption", "encrypted").neverFlows().toVertex()
			.withCharacteristic("Location", "nonEU").create();

	private static final List<Integer> TFG_LENGTH_SCALINGS = List.of(0, 50, 100, 150, 200, 250, 300, 350, 400, 450, 500,
			550);

	private static final List<Integer> TFG_AMOUNT_SCALINGS = List.of(0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000,
			9000, 10000, 11000, 12000, 13000, 14000, 15000);

	private static final List<Integer> CONSTRAINT_SCALINGS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
			16, 17, 18, 19, 20, 40, 60, 80, 100, 120, 140, 160, 180);

	private static final int MEASUREMENT_REPEATS = 3;
	private static final int FLUSH_EVERY = 3;
	private static boolean WARMUP = false;

	@Test
	//@Disabled("Long-running scalability experiment — run via evaluateScalability()")
	void evaluateScalability() throws Throwable {
		Path outDir = Paths.get("testresults");
		Files.createDirectories(outDir);
		Path csv = outDir.resolve(getApproachName().toLowerCase() + "_performance_measurements.json");
		Set<String> done = MeasurementWriter.loadDoneRunIds(csv);

		try (MeasurementWriter writer = new MeasurementWriter(csv, done, FLUSH_EVERY)) {
			scaleTFGLength(writer);
			scaleTFGAmount(writer);
			scaleConstraints(writer);
		}
	}

	
	final AnalysisConstraint encryptedPersonalData = new ConstraintDSL().ofData().withLabel("Sensitivity", "Personal")
			.withoutLabel("Encryption", "Encrypted").neverFlows().toVertex().withCharacteristic("Location", "nonEU")
			.create();
	
	final AnalysisConstraint encryptedNonEu = new ConstraintDSL().ofData().withLabel("Encryption", "Encrypted").neverFlows().toVertex().withCharacteristic("Location", "EU")
			.create();
	
	@Test
	void forwardingEdgeCase() throws Exception {
		var dfdLocation = "models/forwardingEdgeCase.json";
		DataFlowDiagramAndDictionary dfd = new Web2DFDConverter().convert(new WebEditorConverterModel(dfdLocation));
		
		var repairedDFD = getApproach(dfd, List.of(encryptedPersonalData,encryptedNonEu )).repair();
		
		var violations = determineViolations(repairedDFD, List.of(encryptedPersonalData,encryptedNonEu));
		
		if(getApproachName().equals("SMT")) {
		    assertEquals(0, violations);
		}
	}
	
	private void scaleTFGLength(MeasurementWriter writer) throws Throwable {
		for (int scaling : TFG_LENGTH_SCALINGS) {
			RunConfig cfg = RunConfig.forTFG("tfg_length", scaling, 0);
			runWithWarmupAndRepeats(writer, cfg, () -> {
				var dfd = new Scaler(SCALE_DFD).scaleTFGLength(scaling);
				getApproach(dfd, List.of(scalabilityConstraint)).repair();
			});
		}
	}

	private void scaleTFGAmount(MeasurementWriter writer) throws Throwable {
		for (int scaling : TFG_AMOUNT_SCALINGS) {
			RunConfig cfg = RunConfig.forTFG("tfg_amount", 0, scaling);
			runWithWarmupAndRepeats(writer, cfg, () -> {
				var dfd = new Scaler(SCALE_DFD).scaleTFGAmount(scaling);
				getApproach(dfd, List.of(scalabilityConstraint)).repair();
			});
		}
	}

	private void scaleConstraints(MeasurementWriter writer) throws Throwable {
		int dummyLabels = 600;
		for (int s : CONSTRAINT_SCALINGS)
			runConstraintCase(writer, dummyLabels, "constraints_amountConstraint", s, 1, 1, 1, 1);
		for (int s : CONSTRAINT_SCALINGS)
			runConstraintCase(writer, dummyLabels, "constraints_numberWithLabel", 1, s, 1, 1, 1);
		for (int s : CONSTRAINT_SCALINGS)
			runConstraintCase(writer, dummyLabels, "constraints_numberWithoutLabel", 1, 1, s, 1, 1);
		for (int s : CONSTRAINT_SCALINGS)
			runConstraintCase(writer, dummyLabels, "constraints_numberWithCharacteristic", 1, 1, 1, s, 1);
		for (int s : CONSTRAINT_SCALINGS)
			runConstraintCase(writer, dummyLabels, "constraints_numberWithoutCharacteristic", 1, 1, 1, 1, s);
		for (int s : CONSTRAINT_SCALINGS) {
		    int half = (s + 1) / 2;
			runConstraintCase(writer, dummyLabels, "constraints_allTogether", s, half, half, half, half);
		}
	}

	private void runConstraintCase(MeasurementWriter writer, int dummyLabels, String name, int amount, int withLabel,
			int withoutLabel, int withChar, int withoutChar) throws Throwable {
		RunConfig cfg = RunConfig.forConstraints(name, amount, withLabel, withoutLabel, withChar, withoutChar,
				dummyLabels);
		runWithWarmupAndRepeats(writer, cfg, () -> {
			Scaler scaler = new Scaler(SCALE_DFD);
			DataFlowDiagramAndDictionary dfd = scaler.scaleLabels(dummyLabels);
			List<AnalysisConstraint> constraints = scaler.scaleConstraint(amount, withLabel, withoutLabel, withChar,
					withoutChar, dummyLabels);
			getApproach(dfd, constraints).repair();
		});
	}

	private void runWithWarmupAndRepeats(MeasurementWriter writer, RunConfig cfg, RunnableExperiment experiment)
			throws Throwable {
		if (!WARMUP) {
			try {
				experiment.run();
				WARMUP = true;
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		for (int i = 0; i < MEASUREMENT_REPEATS; i++) {
			if (writer.isDone(MeasurementWriter.runId(cfg, "measurement", i)))
				continue;
			timeMeasurement timer = new timeMeasurement();
			try {
				timer.start();
				experiment.run();
				timer.stop();
				writer.append(cfg, timer, "measurement", i);
			} catch (OutOfMemoryError oom) {
				writer.appendFailure(cfg, "measurement", i, "OutOfMemoryError");
				throw oom;
			} catch (Throwable t) {
				String msg = t.getMessage();
				writer.appendFailure(cfg, "measurement", i, t.getClass().getSimpleName() + ": "
						+ (msg != null ? msg.substring(0, Math.min(200, msg.length())) : ""));
				throw t;
			}
		}
	}

	@FunctionalInterface
	private interface RunnableExperiment {
		void run() throws Exception;
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

	public record ViolationResult(String modelName, int violationsBefore, int violationsAfter) {
	}

	public record CostResult(String model, int variant, int approachCost, int tuhhCost) {
	}

	private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws Exception {
		final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
		final String location = Paths.get("scenarios", "dfd", "TUHH-Models").toString();
		return new DataFlowDiagramAndDictionary(PROJECT_NAME,
				Paths.get(location, model, (name + ".dataflowdiagram")).toString(),
				Paths.get(location, model, (name + ".datadictionary")).toString(), Activator.class);
	}
}
