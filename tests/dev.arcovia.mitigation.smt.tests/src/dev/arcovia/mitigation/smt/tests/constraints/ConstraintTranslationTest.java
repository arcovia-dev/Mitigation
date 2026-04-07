package dev.arcovia.mitigation.smt.tests.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.constraints.ConstraintTranslator;
import dev.arcovia.mitigation.smt.preprocess.Preprocess;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;
import dev.arcovia.mitigation.smt.tests.evaluation.ConstraintMapProvider;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

/**
 * This test predates the implementation of selector specific tests. 
 * It simply tests if the formula for any vertex is within the set of allowed formulas.
 *
 */
public class ConstraintTranslationTest {

    @Test
    public void testTranslation() throws Exception {
        Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();
        DataFlowDiagramAndDictionary dfd = ParsingUtils.loadDFD("georgwittberger", "georgwittberger_0");
        List<AnalysisConstraint> constraint = constraintMap.get(7);

        Preprocess preprocess = new Preprocess();
        PreprocessingResult pre = preprocess.preprocess(dfd, constraint, false);
        Config config = new ConfigBuilder().build();

        SMT smt = new SMT(pre, constraint, config);

        ConstraintTranslator translator = new ConstraintTranslator(smt);

        List<String> actualExprs = new ArrayList<>();
        for (DFDVertex vertex : pre.vertices()) {
            actualExprs.add(translator.translateConstraint(constraint.get(0), vertex)
                    .simplify()
                    .toString());
        }

        List<String> expectedExprs = List.of(
                "true",
                String.join("\n",
                        "(not (and (not Pin_37_unset_entrypoint)",
                        "          (not Pin_34_unset_entrypoint)",
                        "          (not (or Pin_34_set_encrypted_connection",
                        "                   Pin_37_set_encrypted_connection",
                        "                   Pin_30_set_encrypted_connection))))"),
                String.join("\n",
                        "(not (and (not Pin_42_unset_entrypoint)",
                        "          (not (or Pin_42_set_encrypted_connection",
                        "                   Pin_30_set_encrypted_connection))))"),
                "true",
                String.join("\n",
                        "(not (and (not Pin_37_unset_entrypoint)",
                        "          (not (or Pin_37_set_encrypted_connection",
                        "                   Pin_30_set_encrypted_connection))))"),
                "true",
                String.join("\n",
                        "(not (and (not Pin_39_unset_entrypoint)",
                        "          (not (or Pin_39_set_encrypted_connection",
                        "                   Pin_30_set_encrypted_connection))))"),
                "true",
                "true");
        Set<String> actual = new HashSet<>(actualExprs);
        assertTrue(actual.containsAll(expectedExprs));
        assertEquals(new HashSet<>(expectedExprs), actual);
    }

}
