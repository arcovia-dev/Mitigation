package dev.arcovia.mitigation.sat.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.examplemodels.Activator;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.sat.Scaler;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

public class ScalerTest {
    public final String MIN_SAT = "models/minsat.json";

    @Test
    public void scaleDFD() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new Scaler(dfd);
        var scaledDfd = scaler.scaleDFD(2, 1);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaled.json");

        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");

        assertTrue(getNumberTFGs(dfd) * 6 <= getNumberTFGs(scaledDfd));
    }

    @Test
    public void scaleLabel() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new Scaler(dfd);
        var scaledDfd = scaler.scaleLabels(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLabels.json");

        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");

        for (int i = 0; i < dfd.dataFlowDiagram()
                .getNodes()
                .size(); i++) {
            var node = scaledDfd.dataFlowDiagram()
                    .getNodes()
                    .get(i);
            var unscaledNode = dfd.dataFlowDiagram()
                    .getNodes()
                    .get(i);

            var labelNames = getLabelNames(node);
            labelNames.removeAll(getLabelNames(unscaledNode));

            assertEquals(List.of("dummy_0", "dummy_1"), labelNames);
        }
    }

    @Test
    public void scaleLabelTyepes() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new Scaler(dfd);
        var scaledDfd = scaler.scaleLabelTypes(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLabelTypes.json");

        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");

        for (int i = 0; i < dfd.dataFlowDiagram()
                .getNodes()
                .size(); i++) {
            var node = scaledDfd.dataFlowDiagram()
                    .getNodes()
                    .get(i);
            var unscaledNode = dfd.dataFlowDiagram()
                    .getNodes()
                    .get(i);

            assertTrue(node.getProperties()
                    .size() - 2 == unscaledNode.getProperties()
                            .size());
        }
        var labelTypes = getLabelTypeNames(scaledDfd);
        labelTypes.removeAll(getLabelTypeNames(dfd));

        assertEquals(List.of("dummyType_0", "dummyType_1"), labelTypes);
    }

    @Test
    public void scaleTFGLength() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new Scaler(dfd);
        var scaledDfd = scaler.scaleTFGLength(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLength.json");

        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");

        var nodeNames = getNodeNames(scaledDfd);
        nodeNames.removeAll(getNodeNames(dfd));

        assertEquals(List.of("dummyNode_0", "dummyNode_1"), nodeNames);

    }

    @Test
    public void scaleTFGNumber() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new Scaler(dfd);
        var scaledDfd = scaler.scaleTFGAmount(1);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledTFGAmount.json");
        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");

        assertTrue(getNumberTFGs(dfd) * 2 <= getNumberTFGs(scaledDfd));
    }

    @Test
    public void scaleAll() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new Scaler(dfd);
        var scaledDfd = scaler.scaleLabels(20);

        scaledDfd = scaler.scaleLabelTypes(20);

        scaledDfd = scaler.scaleTFGLength(5);
        scaledDfd = scaler.scaleTFGAmount(5);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledALL.json");
    }
    
    @Test
    public void scaleConstraints() {
        var scaler = new Scaler();
        var constraints = scaler.scaleConstraint(500, 50, 50, 50, 5000);
        
        for (var constraint : constraints) {
            var translation = new CNFTranslation(constraint);
            translation.constructCNF();
        }
        
        assertTrue(constraints.size() == 500);
    }

    private DataFlowDiagramAndDictionary loadDFD(String model, String name) throws StandaloneInitializationException {
        final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
        final String location = Paths.get("scenarios", "dfd", "TUHH-Models")
                .toString();
        return new DataFlowDiagramAndDictionary(PROJECT_NAME, Paths.get(location, model, (name + ".dataflowdiagram"))
                .toString(),
                Paths.get(location, model, (name + ".datadictionary"))
                        .toString(),
                Activator.class);
    }

    private int getNumberTFGs(DataFlowDiagramAndDictionary dfd) {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();

        return flowGraph.getTransposeFlowGraphs()
                .size();
    }

    private List<String> getNodeNames(DataFlowDiagramAndDictionary dfd) {
        var nodes = dfd.dataFlowDiagram()
                .getNodes();
        List<String> names = new ArrayList<>();
        for (var node : nodes) {
            names.add(node.getEntityName());
        }
        return names;
    }

    private List<String> getLabelTypeNames(DataFlowDiagramAndDictionary dfd) {
        var types = dfd.dataDictionary()
                .getLabelTypes();
        List<String> names = new ArrayList<>();
        for (var type : types) {
            names.add(type.getEntityName());
        }
        return names;
    }

    private List<String> getLabelNames(Node node) {
        List<String> names = new ArrayList<>();

        for (var label : node.getProperties()) {
            names.add(label.getEntityName());
        }

        return names;
    }

}
