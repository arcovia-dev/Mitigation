package dev.arcovia.mitigation.sat.tests;

import org.dataflowanalysis.converter.dfd2web.DFD2WebConverter;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.sat.DFDScaler;

public class ScalerTest {
    public final String MIN_SAT = "models/minsat.json";
    
    
    @Test
    public void scale() {
        var scaledDfd = DFDScaler.scaleDFD(MIN_SAT);
        
        var dfdConverter = new DFD2WebConverter();
        dfdConverter.convert(scaledDfd)
        .save("testresults/", "minsat-scaled.json");
    }
}
