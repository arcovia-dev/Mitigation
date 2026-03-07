package dev.arcovia.mitigation.smt.tests.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.config.CostConfig;
import dev.arcovia.mitigation.smt.config.CostConfigBuilder;

class CostConfigBuilderTest {

    private static final dataflowdiagramFactory dfdFactory = dataflowdiagramFactory.eINSTANCE;
    private static final datadictionaryFactory ddFactory = datadictionaryFactory.eINSTANCE;

    @Test
    void builderDefaultBuildHasEmptyMapsAndWeighFalse() {
        CostConfig config = new CostConfigBuilder().build();

        assertNotNull(config.addLabelCost());
        assertNotNull(config.removeLabelCost());
        assertNotNull(config.nodeFactor());
        assertNotNull(config.pinFactor());

        assertTrue(config.addLabelCost()
                .isEmpty());
        assertTrue(config.removeLabelCost()
                .isEmpty());
        assertTrue(config.nodeFactor()
                .isEmpty());
        assertTrue(config.pinFactor()
                .isEmpty());

        assertFalse(config.weighTFGs());
    }

    @Test
    void withLabelCostSetsBothAddAndRemoveToSameMap() {
        HashMap<String, Integer> labelCost = new HashMap<>();
        labelCost.put("A.B", 7);

        CostConfig config = new CostConfigBuilder().withLabelCost(labelCost)
                .build();

        assertSame(labelCost, config.addLabelCost());
        assertSame(labelCost, config.removeLabelCost());
        assertEquals(7, config.addLabelCost()
                .get("A.B"));
        assertEquals(7, config.removeLabelCost()
                .get("A.B"));
    }

    @Test
    void withAddAndRemoveLabelCostSetsIndependently() {
        HashMap<String, Integer> add = new HashMap<>();
        add.put("A.B", 1);

        HashMap<String, Integer> rem = new HashMap<>();
        rem.put("A.B", 2);

        CostConfig config = new CostConfigBuilder().withAddLabelCost(add)
                .withRemoveLabelCost(rem)
                .build();

        assertSame(add, config.addLabelCost());
        assertSame(rem, config.removeLabelCost());
        assertEquals(1, config.addLabelCost()
                .get("A.B"));
        assertEquals(2, config.removeLabelCost()
                .get("A.B"));
    }

    @Test
    void withNodeFactorAndWithPinFactorAreApplied() {
        Node n1 = dfdFactory.createProcess();
        Pin p1 = ddFactory.createPin();

        HashMap<Node, Integer> nodeFactor = new HashMap<>();
        nodeFactor.put(n1, 3);

        HashMap<Pin, Integer> pinFactor = new HashMap<>();
        pinFactor.put(p1, 4);

        CostConfig config = new CostConfigBuilder().withNodeFactor(nodeFactor)
                .withPinFactor(pinFactor)
                .build();

        assertSame(nodeFactor, config.nodeFactor());
        assertSame(pinFactor, config.pinFactor());
        assertEquals(3, config.nodeFactor()
                .get(n1));
        assertEquals(4, config.pinFactor()
                .get(p1));
    }

    @Test
    void weighTFGsSetsFlag() {
        CostConfig config = new CostConfigBuilder().weighTFGs(true)
                .build();
        assertTrue(config.weighTFGs());
    }

    @Test
    void chainingLastWriteWins() {
        HashMap<String, Integer> m1 = new HashMap<>();
        m1.put("X", 1);

        HashMap<String, Integer> m2 = new HashMap<>();
        m2.put("X", 2);

        CostConfig config = new CostConfigBuilder().withLabelCost(m1)
                .withAddLabelCost(m2) // override only add
                .build();

        assertSame(m2, config.addLabelCost());
        assertSame(m1, config.removeLabelCost());
        assertEquals(2, config.addLabelCost()
                .get("X"));
        assertEquals(1, config.removeLabelCost()
                .get("X"));
    }
}
