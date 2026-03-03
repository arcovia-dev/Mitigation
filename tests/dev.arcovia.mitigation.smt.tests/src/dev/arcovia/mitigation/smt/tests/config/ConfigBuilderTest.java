package dev.arcovia.mitigation.smt.tests.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.config.CostConfig;
import dev.arcovia.mitigation.smt.config.CostConfigBuilder;

class ConfigBuilderTest {

    @Test
    void builder_defaultsMatchConstructorDefaults() {
        Config cfg = new ConfigBuilder().build();

        assertTrue(cfg.isOnlyRelevantModifications());
        assertTrue(cfg.isAddNodeLabels());
        assertTrue(cfg.isRemoveNodeLabels());
        assertTrue(cfg.isAddDataLabels());
        assertTrue(cfg.isRemoveDataLabels());

        assertNotNull(cfg.getCostConfig());

        assertFalse(cfg.isCheckForViolationsAfter());
        assertFalse(cfg.isFindExpressionTreeSize());
        assertFalse(cfg.isOnlyViolatingTFGs());
    }

    @Test
    void builderAppliesAllFlagsAndCostConfig() {
        CostConfig cost = new CostConfigBuilder().weighTFGs(true)
                .build();

        Config cfg = new ConfigBuilder().onlyRelevantModifications(false)
                .addNodeLabels(false)
                .removeNodeLabels(false)
                .addDataLabels(false)
                .removeDataLabels(false)
                .costConfig(cost)
                .checkForViolationsAfter(true)
                .findExpressionTreeSize(true)
                .onlyViolatingTFGs(true)
                .build();

        assertFalse(cfg.isOnlyRelevantModifications());
        assertFalse(cfg.isAddNodeLabels());
        assertFalse(cfg.isRemoveNodeLabels());
        assertFalse(cfg.isAddDataLabels());
        assertFalse(cfg.isRemoveDataLabels());

        assertSame(cost, cfg.getCostConfig());

        assertTrue(cfg.isCheckForViolationsAfter());
        assertTrue(cfg.isFindExpressionTreeSize());
        assertTrue(cfg.isOnlyViolatingTFGs());
    }

    @Test
    void builderLastWriteWins() {
        CostConfig cost1 = new CostConfigBuilder().weighTFGs(false)
                .build();
        CostConfig cost2 = new CostConfigBuilder().weighTFGs(true)
                .build();

        Config cfg = new ConfigBuilder().onlyRelevantModifications(false)
                .onlyRelevantModifications(true)
                .costConfig(cost1)
                .costConfig(cost2)
                .checkForViolationsAfter(false)
                .checkForViolationsAfter(true)
                .findExpressionTreeSize(false)
                .findExpressionTreeSize(true)
                .build();

        assertTrue(cfg.isOnlyRelevantModifications());
        assertSame(cost2, cfg.getCostConfig());
        assertTrue(cfg.isCheckForViolationsAfter());
        assertTrue(cfg.isFindExpressionTreeSize());
    }
}
