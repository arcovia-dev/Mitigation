package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.context.DSLContext;
import org.dataflowanalysis.analysis.dsl.selectors.VariableNameSelector;
import org.dataflowanalysis.analysis.utils.StringView;

public class DataNameTest extends AbstractSelectorConstraintTest {

    @Override
    protected Stream<SelectorTestCase> cases() {
        return dataNameTest();
    }

    static Stream<SelectorTestCase> dataNameTest() {

        DSLContext context = new DSLContext();
        VariableNameSelector matches = VariableNameSelector.fromString(new StringView("named " + FLOW), context)
                .getResult();
        VariableNameSelector notMatches = VariableNameSelector.fromString(new StringView("named otherName"), context)
                .getResult();

        AnalysisConstraint firstConstr = new AnalysisConstraint("default");
        firstConstr.addDataSourceSelector(matches);

        AnalysisConstraint secondConstr = new AnalysisConstraint("default");
        secondConstr.addDataSourceSelector(notMatches);

        return Stream.of(
                new SelectorTestCase("with right flow name -> false", firstConstr, List.of(TRUE), List.of(FALSE)),
                new SelectorTestCase("with wrong flow name -> true", secondConstr, List.of(TRUE), List.of(TRUE)));
    }
}
