package dev.arcovia.mitigation.sat.tests;

import java.nio.file.Paths;

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
    }

    @Test
    public void scaleLabel() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleLabels(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLabels.json");
    }

    @Test
    public void scaleLabelTyepes() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleLabelTypes(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLabelTypes.json");
    }

    @Test
    public void scaleTFGLength() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleTFGLength(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledLength.json");
    }

    @Test
    public void scaleTFGNumber() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleTFGAmount(2);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledTFGAmount.json");
    }

    @Test
    public void scaleAll() throws StandaloneInitializationException {
        var dfd = loadDFD("mudigal-technologies", "mudigal-technologies_7");
        var scaler = new DFDScaler(dfd);
        var scaledDfd = scaler.scaleLabels(20);

        scaledDfd = scaler.scaleLabelTypes(20);
        scaledDfd = scaler.scaleTFGAmount(10);
        scaledDfd = scaler.scaleTFGLength(5);

        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
                .save("testresults/", "scaledALL.json");
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
}
