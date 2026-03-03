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
        CostConfig cfg = new CostConfigBuilder().build();

        assertNotNull(cfg.getAddLabelCost());
        assertNotNull(cfg.getRemoveLabelCost());
        assertNotNull(cfg.getNodeFactor());
        assertNotNull(cfg.getPinFactor());

        assertTrue(cfg.getAddLabelCost()
                .isEmpty());
        assertTrue(cfg.getRemoveLabelCost()
                .isEmpty());
        assertTrue(cfg.getNodeFactor()
                .isEmpty());
        assertTrue(cfg.getPinFactor()
                .isEmpty());

        assertFalse(cfg.isWeighTFGs());
    }

    @Test
    void withLabelCostSetsBothAddAndRemoveToSameMap() {
        HashMap<String, Integer> labelCost = new HashMap<>();
        labelCost.put("A.B", 7);

        CostConfig cfg = new CostConfigBuilder().withLabelCost(labelCost)
                .build();

        assertSame(labelCost, cfg.getAddLabelCost());
        assertSame(labelCost, cfg.getRemoveLabelCost());
        assertEquals(7, cfg.getAddLabelCost()
                .get("A.B"));
        assertEquals(7, cfg.getRemoveLabelCost()
                .get("A.B"));
    }

    @Test
    void withAddAndRemoveLabelCostSetsIndependently() {
        HashMap<String, Integer> add = new HashMap<>();
        add.put("A.B", 1);

        HashMap<String, Integer> rem = new HashMap<>();
        rem.put("A.B", 2);

        CostConfig cfg = new CostConfigBuilder().withAddLabelCost(add)
                .withRemoveLabelCost(rem)
                .build();

        assertSame(add, cfg.getAddLabelCost());
        assertSame(rem, cfg.getRemoveLabelCost());
        assertEquals(1, cfg.getAddLabelCost()
                .get("A.B"));
        assertEquals(2, cfg.getRemoveLabelCost()
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

        CostConfig cfg = new CostConfigBuilder().withNodeFactor(nodeFactor)
                .withPinFactor(pinFactor)
                .build();

        assertSame(nodeFactor, cfg.getNodeFactor());
        assertSame(pinFactor, cfg.getPinFactor());
        assertEquals(3, cfg.getNodeFactor()
                .get(n1));
        assertEquals(4, cfg.getPinFactor()
                .get(p1));
    }

    @Test
    void weighTFGsSetsFlag() {
        CostConfig cfg = new CostConfigBuilder().weighTFGs(true)
                .build();
        assertTrue(cfg.isWeighTFGs());
    }

    @Test
    void chainingLastWriteWins() {
        HashMap<String, Integer> m1 = new HashMap<>();
        m1.put("X", 1);

        HashMap<String, Integer> m2 = new HashMap<>();
        m2.put("X", 2);

        CostConfig cfg = new CostConfigBuilder().withLabelCost(m1)
                .withAddLabelCost(m2) // override only add
                .build();

        assertSame(m2, cfg.getAddLabelCost());
        assertSame(m1, cfg.getRemoveLabelCost());
        assertEquals(2, cfg.getAddLabelCost()
                .get("X"));
        assertEquals(1, cfg.getRemoveLabelCost()
                .get("X"));
    }
}
