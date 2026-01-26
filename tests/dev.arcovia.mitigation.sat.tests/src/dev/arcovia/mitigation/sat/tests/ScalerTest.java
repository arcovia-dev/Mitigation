package dev.arcovia.mitigation.sat.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.sat.DFDScaler;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

public class ScalerTest {
    public final String MIN_SAT = "models/minsat.json";

    @Test
    public void scaleDFD() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
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
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleLabels(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLabels.json");
        
        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
              
        for (int i = 0; i< dfd.dataFlowDiagram().getNodes().size(); i++) {
            var node = scaledDfd.dataFlowDiagram().getNodes().get(i);
            var unscaledNode = dfd.dataFlowDiagram().getNodes().get(i);
            
            assertTrue(node.getProperties().size() - 2 == unscaledNode.getProperties().size());
        }
    }

    @Test
    public void scaleLabelTyepes() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleLabelTypes(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLabelTypes.json");
        
        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        
        for (int i = 0; i< dfd.dataFlowDiagram().getNodes().size(); i++) {
            var node = scaledDfd.dataFlowDiagram().getNodes().get(i);
            var unscaledNode = dfd.dataFlowDiagram().getNodes().get(i);
            
            assertTrue(node.getProperties().size() - 2 == unscaledNode.getProperties().size());
        }
        
        
        
        assertTrue(dfd.dataDictionary().getLabelTypes().size() + 2 == scaledDfd.dataDictionary().getLabelTypes().size());
    }

    @Test
    public void scaleTFGLength() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleTFGLength(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLength.json");
        
        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        assertTrue(getMaxTFGLength(dfd) + 2 == getMaxTFGLength(scaledDfd));
        
    }

    @Test
    public void scaleTFGNumber() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleTFGAmount(1);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledTFGAmount.json");
        dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        
        System.out.println("nummmer" + getNumberTFGs(dfd));
        System.out.println("nummmer" + getNumberTFGs(scaledDfd));
        assertTrue(getNumberTFGs(dfd) * 2 <= getNumberTFGs(scaledDfd));
    }

    @Test
    public void scaleAll() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleLabels(20);

        scaledDfd = scaler.scaleLabelTypes(20);
        
        scaledDfd = scaler.scaleTFGLength(5);
        scaledDfd = scaler.scaleTFGAmount(5);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledALL.json");
        System.out.println("nummmer" + getNumberTFGs(scaledDfd));
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
    private int getMaxTFGLength(DataFlowDiagramAndDictionary dfd) {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        
        int length = 0;
        
        for (var tfg : flowGraph.getTransposeFlowGraphs()) {
            if (tfg.getVertices().size() > length) {
                length = tfg.getVertices().size();
            }
        }
        
        return length;
    }
    private int getNumberTFGs(DataFlowDiagramAndDictionary dfd) {
        var resourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(), dfd.dataFlowDiagram());
        var analysis = new DFDDataFlowAnalysisBuilder().standalone()
                .useCustomResourceProvider(resourceProvider)
                .build();

        analysis.initializeAnalysis();
        var flowGraph = analysis.findFlowGraphs();
        flowGraph.evaluate();
        
        return flowGraph.getTransposeFlowGraphs().size();
    }
}
