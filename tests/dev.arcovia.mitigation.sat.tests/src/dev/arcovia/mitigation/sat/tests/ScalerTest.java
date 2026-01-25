package dev.arcovia.mitigation.sat.tests;

import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.sat.DFDScaler;

public class ScalerTest {
    public final String MIN_SAT = "models/minsat.json";
    
    
    @Test
    public void scale() {
        var scaler = new DFDScaler(MIN_SAT);
        var scaledDfd = scaler.scaleDFD(2, 1);
                
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
        .save("testresults/", "minsat-scaled.json");
    }
    
    @Test
    public void scaleLabel() {
        var scaler = new DFDScaler(MIN_SAT);
        var scaledDfd = scaler.scaleLabels(2);
                
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
        .save("testresults/", "minsat-scaledLabels.json");
    }
    
    @Test
    public void scaleLabelTyepes() {
        var scaler = new DFDScaler(MIN_SAT);
        var scaledDfd = scaler.scaleLabelTypes(2);
                
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
        .save("testresults/", "minsat-scaledLabelTypes.json");
    }
    
    @Test
    public void scaleTFGLentgth() {
        var scaler = new DFDScaler(MIN_SAT);
        var scaledDfd = scaler.scaleLTFGLength(2);
                
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
        .save("testresults/", "minsat-scaledLength.json");
    }
    @Test
    public void scaleTFGNumber() {
        var scaler = new DFDScaler(MIN_SAT);
        var scaledDfd = scaler.scaleLTFGAmount(2);
                
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
        .save("testresults/", "minsat-scaledTFGAmount.json");
    }
    
    @Test
    public void scaleAll() {
        var scaler = new DFDScaler(MIN_SAT);
        var scaledDfd = scaler.scaleLabels(20);
        
        scaler = new DFDScaler(scaledDfd);
        scaledDfd = scaler.scaleLabelTypes(20);
        scaledDfd = scaler.scaleLTFGAmount(10);
        scaledDfd = scaler.scaleLTFGLength(5);
                
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
        .save("testresults/", "minsat-scaledALL.json");
    }
}
