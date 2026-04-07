package dev.arcovia.mitigation.smt;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.preprocess.Preprocess;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

/**
 * This class provides a static entrypoint into the solver. Given a DFD, a list of constraints and a config it returns a
 * repaired DFD.
 */
public class Mitigation {

    /**
     * Repairs DFD. First it preprocesses, using existing DFA tooling, encodes into Z3 and finally repairs
     * @param dfd Input dataflow Diagram
     * @param constraints Constraints that the output needs to adhere too
     * @param config Chosen config. If null, the default config is used.
     * @throws StandaloneInitializationException If input DFD is incorrect
     */
    public static SolvingResult run(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints, Config config)
            throws StandaloneInitializationException {
        if (config == null) {
            config = new ConfigBuilder().build();
        }
        Preprocess preprocces = new Preprocess();
        PreprocessingResult preprocessingResult = preprocces.preprocess(dfd, constraints, config.onlyViolatingTFGs());
        SMT smt = new SMT(preprocessingResult, constraints, config);
        return smt.repair();
    }
}
