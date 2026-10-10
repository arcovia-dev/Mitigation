package dev.arcovia.mitigation.uncertainty.evaluation.fixtures;

import dev.abunai.confidentiality.analysis.model.uncertainty.UncertaintySource;
import dev.arcovia.mitigation.cost.ActionType;
import dev.arcovia.mitigation.ilp.*;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.uncertainty.loading.LoadedUncertaintyModel;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelLoader;
import dev.arcovia.mitigation.uncertainty.loading.UncertaintyModelSpec;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.converter.web2dfd.Web2DFDConverter;
import org.dataflowanalysis.converter.web2dfd.WebEditorConverterModel;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The subjects of the scalability drivers that do not use a generated uncertainty model.
 */
public final class ScalabilitySubjectFixtures {

    /**
     * The published architecture's declared sources, in the order the sweep adds them. Twenty-one
     * were declared by the model's authors; the sweep takes a prefix of this list.
     */
    private static final List<String> PUBLISHED_SOURCE_ORDER = List.of(
            "ratings1", "movie_catalog2", "Movie_info3", "AuthServer4", "movie_catalog5",
            "Info6", "ratings7", "ratings1_1", "movie_catalog2_1", "Movie_info3_1",
            "AuthServer4_1", "movie_catalog5_1", "Info6_1", "ratings7_1", "ratings1_2",
            "movie_catalog2_2", "Movie_info3_2", "AuthServer4_2", "movie_catalog5_2",
            "Info6_2", "ratings7_2");

    private ScalabilitySubjectFixtures() {
    }

    /**
     * Number of uncertainty sources the published architecture declares.
     *
     * @return the declared source count
     */
    public static int publishedSourceLimit() {
        return PUBLISHED_SOURCE_ORDER.size();
    }

    /**
     * Loads the koushikkothagal microSecEnD architecture with all of its declared sources.
     *
     * @return the loaded model the repair pipeline consumes
     */
    public static LoadedUncertaintyModel publishedModel() {
        Path folder = Path.of(System.getProperty("user.dir")).getParent()
                .resolve("dev.arcovia.mitigation.ranking.tests").resolve("models").resolve("koushikkothagal21");
        return new UncertaintyModelLoader().load(new UncertaintyModelSpec(
                folder.resolve("koushikkothagal.dataflowdiagram"),
                folder.resolve("koushikkothagal.datadictionary"),
                folder.resolve("koushikkothagal.uncertainty")));
    }

    /**
     * Selects the first {@code count} declared sources of the published architecture.
     *
     * @param model the loaded published model
     * @param count how many sources to select
     * @return the selected sources, in declaration order
     * @throws IllegalStateException when the model does not declare one of the expected sources
     */
    public static List<UncertaintySource> publishedSources(LoadedUncertaintyModel model, int count) {
        return PUBLISHED_SOURCE_ORDER.subList(0, count).stream()
                .map(name -> model.sources().stream()
                        .filter(source -> source.getEntityName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Source not declared in fixture: " + name)))
                .toList();
    }

    public static List<Constraint> getAuthenticationConstraints() {
        return List.of(new Constraint(new ConstraintDSL().ofData()
                .withoutLabel("Stereotype", "authenticated_request")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Stereotype", "internal")
                .create()));
    }

    /**
     * Builds a model of {@code motifCount} copies of one acyclic motif. The copies share no
     * identifier, so the model grows without changing the shape of any single flow graph.
     *
     * @param motifCount how many copies to include
     * @return the assembled model
     */
    public static DataFlowDiagramAndDictionary duplicatedMotifs(int motifCount) {
        var result = copy(loadMotif());
        renameIds(result, "motif-0");
        IntStream.range(1, motifCount).forEach(motif -> {
            var addition = copy(loadMotif());
            renameIds(addition, "motif-" + motif);
            result.dataFlowDiagram().getNodes().addAll(addition.dataFlowDiagram().getNodes());
            result.dataFlowDiagram().getFlows().addAll(addition.dataFlowDiagram().getFlows());
            result.dataDictionary().getBehavior().addAll(addition.dataDictionary().getBehavior());
            result.dataDictionary().getLabelTypes().addAll(addition.dataDictionary().getLabelTypes());
        });
        return result;
    }

    /**
     * The residency rule the duplicated-motif model is evaluated against.
     *
     * @return the single constraint, carrying the node-deletion strategy the motif needs
     */
    public static List<Constraint> motifConstraints() {
        return List.of(new Constraint(new ConstraintDSL().ofData()
                .withLabel("Sensitivity", "Personal")
                .neverFlows()
                .toVertex()
                .withCharacteristic("Location", "nonEU")
                .create(), List.of(new MitigationStrategy(
                List.of(new NodeLabel(new Label("Stereotype", "internal"))), 1, MitigationType.DeleteNode))));
    }

    /**
     * Builds {@code actionCount} distinct candidate actions that all repair the same single
     * violation, so the solver sees one coverage row over a growing action space.
     *
     * @param actionCount how many candidates to construct
     * @return the candidate actions
     */
    public static List<Mitigation> candidateActions(int actionCount) {
        return IntStream.range(0, actionCount)
                .mapToObj(index -> new Mitigation(new ActionTerm("metric-action-" + index,
                        List.of(new NodeLabel(new Label("Metric", "candidate-" + index))), ActionType.Adding),
                        0, List.of()))
                .toList();
    }

    private static DataFlowDiagramAndDictionary loadMotif() {
        var model = Path.of(System.getProperty("user.dir")).resolve("models").resolve("minsat.json");
        return new Web2DFDConverter().convert(new WebEditorConverterModel(model.toString()));
    }

    private static DataFlowDiagramAndDictionary copy(DataFlowDiagramAndDictionary model) {
        var copier = new EcoreUtil.Copier();
        var diagram = (DataFlowDiagram) copier.copy(model.dataFlowDiagram());
        var dictionary = (DataDictionary) copier.copy(model.dataDictionary());
        copier.copyReferences();
        return new DataFlowDiagramAndDictionary(diagram, dictionary);
    }

    private static void renameIds(DataFlowDiagramAndDictionary model, String suffix) {
        renameIds(model.dataFlowDiagram(), suffix);
        renameIds(model.dataDictionary(), suffix);
    }

    private static void renameIds(EObject root, String suffix) {
        List<EObject> objects = new ArrayList<>();
        objects.add(root);
        var descendants = EcoreUtil.getAllContents(root, true);
        while (descendants.hasNext()) {
            objects.add((EObject) descendants.next());
        }
        objects.forEach(object -> {
            var id = object.eClass().getEStructuralFeature("id");
            if (id != null && object.eGet(id) instanceof String value && !value.isBlank()) {
                object.eSet(id, value + "-" + suffix);
            }
        });
    }
}
