package dev.arcovia.mitigation.smt.tests.constraints;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.constraints.ConstraintTranslator;
import dev.arcovia.mitigation.smt.preprocess.Preprocess;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;

/**
 * In general the tests in this package focus on ensuring correct selector translation logic. As the concrete encoding
 * functions of these classes are not public we resort to testing Constraints that contain single selectors instead. As
 * the formulas can blow up pretty fast even for small cases, we use a minimal DFD as a test case. It includes two nodes
 * that are connected by a flow with a Set assignment. We compare results with expected results using String comparison.
 * Therefore the tests can break if the Names of the expressions in our implementation re changed or Z3 changes their
 * naming scheme. To mitigate the risk of typos we defined relevant string constants below and use them when creating
 * the naming schemes of expected output. As some cases allow for multiple changes, e.g. one of two labels need to be
 * added or two labels need to be added the resulting formula is not deterministic. Therefore we provide all possible
 * equivalent formulas and check if the output is one of them. 
 * 
 * About the annotation @TestInstance(TestInstance.Lifecycle.PER_CLASS): Usually JUnit Test Instance lifecycle is per method
 * instead of per class. However @ParameterizedTest that have a per method lifecycle can only have static
 * @MethodSource methods. This package uses a cases() method as method source. As this class is extended by the test
 * classes for individual selectors, its cases() method can not be static as static+abstract are incompatible. Therefore
 * we change the lifecycle via annotation to allow non-static method sources.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractSelectorConstraintTest {

    protected abstract Stream<SelectorTestCase> cases();

    protected static final String LABELTYPE = "dummyType";
    protected static final String LABEL1 = "dummyLabel1";
    protected static final String LABEL2 = "dummyLabel2";
    protected static final String LABEL3 = "dummyLabel3";
    protected static final String LABEL4 = "dummyLabel4";
    protected static final String SOURCE = "source";
    protected static final String SINK = "sink";
    protected static final String FLOW = "sourceToSink";
    protected static final String PIN = "pin1";
    protected static final String TRUE = "true";
    protected static final String FALSE = "false";

    protected static String set(String labelName) {
        return "Pin_" + PIN + "_set_" + labelName;
    }

    protected static String unset(String labelName) {
        return "Pin_" + PIN + "_unset_" + labelName;
    }

    protected static String nodeLabel(String nodeName, String labelName) {
        return nodeName + "_label_" + labelName;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    public void selectorConstraintTranslatesAsExpected(SelectorTestCase testCase) {
        DataFlowDiagramAndDictionary dfdAndDD = DFDProvider.buildTestCase();

        AnalysisConstraint constraint = testCase.constraint();
        List<AnalysisConstraint> constraints = new ArrayList<>();
        constraints.add(constraint);

        Preprocess preprocess = new Preprocess();
        PreprocessingResult pre = preprocess.preprocess(dfdAndDD, constraints, false);

        Config config = new ConfigBuilder().build();
        SMT smt = new SMT(pre, constraints, config);

        ConstraintTranslator translator = new ConstraintTranslator(smt);
        Set<DFDVertex> vertices = pre.vertices();

        Node source = dfdAndDD.dataFlowDiagram()
                .getNodes()
                .stream()
                .filter(x -> x.getEntityName()
                        .equals("source"))
                .findFirst()
                .orElseThrow();

        DFDVertex sourceVertex = vertices.stream()
                .filter(x -> x.getReferencedElement()
                        .equals(source))
                .findFirst()
                .orElseThrow();

        Node sink = dfdAndDD.dataFlowDiagram()
                .getNodes()
                .stream()
                .filter(x -> x.getEntityName()
                        .equals("sink"))
                .findFirst()
                .orElseThrow();

        DFDVertex sinkVertex = vertices.stream()
                .filter(x -> x.getReferencedElement()
                        .equals(sink))
                .findFirst()
                .orElseThrow();

        String sourceFormula = ((BoolExpr) translator.translateConstraint(constraint, sourceVertex)
                .simplify()).toString();
        assertTrue(testCase.validSourceFormulas()
                .contains(sourceFormula),
                () -> "Unexpected source formula: " + sourceFormula.toString() + " \nExpected one of: " + testCase.validSourceFormulas() + "\nActual: "
                        + sourceFormula);
        String sinkFormula = ((BoolExpr) translator.translateConstraint(constraint, sinkVertex)
                .simplify()).toString();
        assertTrue(testCase.validSinkFormulas()
                .contains(sinkFormula),
                () -> "Unexpected sink formula: " + sinkFormula.toString() + "\nExpected one of: " + testCase.validSinkFormulas() + "\nActual: "
                        + sinkFormula);

    }
}