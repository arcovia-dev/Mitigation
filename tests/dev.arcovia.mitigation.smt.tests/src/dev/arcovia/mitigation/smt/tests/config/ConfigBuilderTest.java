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
    void builderDefaultsMatchConstructorDefaults() {
        Config config = new ConfigBuilder().build();

        assertTrue(config.onlyRelevantModifications());
        assertTrue(config.addNodeLabels());
        assertTrue(config.removeNodeLabels());
        assertTrue(config.addDataLabels());
        assertTrue(config.removeDataLabels());

        assertNotNull(config.costConfig());

        assertFalse(config.checkForViolationsAfter());
        assertFalse(config.findExpressionTreeSize());
        assertFalse(config.onlyViolatingTFGs());
    }

    @Test
    void builderAppliesAllFlagsAndCostConfig() {
        CostConfig cost = new CostConfigBuilder().weighTFGs(true)
                .build();

        Config config = new ConfigBuilder().onlyRelevantModifications(false)
                .addNodeLabels(false)
                .removeNodeLabels(false)
                .addDataLabels(false)
                .removeDataLabels(false)
                .costConfig(cost)
                .checkForViolationsAfter(true)
                .findExpressionTreeSize(true)
                .onlyViolatingTFGs(true)
                .build();

        assertFalse(config.onlyRelevantModifications());
        assertFalse(config.addNodeLabels());
        assertFalse(config.removeNodeLabels());
        assertFalse(config.addDataLabels());
        assertFalse(config.removeDataLabels());

        assertSame(cost, config.costConfig());

        assertTrue(config.checkForViolationsAfter());
        assertTrue(config.findExpressionTreeSize());
        assertTrue(config.onlyViolatingTFGs());
    }

    @Test
    void builderLastWriteWins() {
        CostConfig cost1 = new CostConfigBuilder().weighTFGs(false)
                .build();
        CostConfig cost2 = new CostConfigBuilder().weighTFGs(true)
                .build();

        Config config = new ConfigBuilder().onlyRelevantModifications(false)
                .onlyRelevantModifications(true)
                .costConfig(cost1)
                .costConfig(cost2)
                .checkForViolationsAfter(false)
                .checkForViolationsAfter(true)
                .findExpressionTreeSize(false)
                .findExpressionTreeSize(true)
                .build();

        assertTrue(config.onlyRelevantModifications());
        assertSame(cost2, config.costConfig());
        assertTrue(config.checkForViolationsAfter());
        assertTrue(config.findExpressionTreeSize());
    }
}
