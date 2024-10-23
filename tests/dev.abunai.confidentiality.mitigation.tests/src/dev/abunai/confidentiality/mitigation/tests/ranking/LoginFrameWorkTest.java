package dev.abunai.confidentiality.mitigation.tests.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.junit.jupiter.api.Test;

import dev.abunai.confidentiality.analysis.core.UncertainConstraintViolation;
import dev.abunai.confidentiality.analysis.dfd.DFDUncertainFlowGraphCollection;
import dev.abunai.confidentiality.analysis.tests.DFDTestBase;
import dev.abunai.confidentiality.mitigation.ranking.RankerType;
import dev.abunai.confidentiality.mitigation.ranking.RankingAggregationMethod;
import dev.abunai.confidentiality.mitigation.tests.MitigationStrategy;
import dev.abunai.confidentiality.mitigation.tests.MitigationTestBase;


public class LoginFrameWorkTest  extends MitigationTestBase{
    
    protected String getFolderName() {
        return "LoginFramework";
    }

    protected String getFilesName() {
        return "default";
    }
    
    protected String customPythonPath() {
        return "C:\\Python310\\python.exe";
    }

    protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
        List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
        constraints.add(it -> {
            return this.retrieveNodeLabels(it).contains("external")
                    && !(this.retrieveAllDataLabels(it).contains("trusted") || this.retrieveNodeLabels(it).contains("trusted"));
        });
        constraints.add(it -> {
            return this.retrieveNodeLabels(it).contains("external")
                    && this.retrieveAllDataLabels(it).contains("bank") && !this.retrieveAllDataLabels(it).contains("mask");
        });
        constraints.add(it -> {
            return this.retrieveNodeLabels(it).contains("logs")
                     && ! (this.retrieveAllDataLabels(it).contains("log_sanitize") ||this.retrieveNodeLabels(it).contains("log_sanitize"));
        });
        constraints.add(it -> {
            return this.retrieveNodeLabels(it).contains("internal")
                     && !this.retrieveNodeLabels(it).contains("local_logging");
        });
        
        return constraints;
    }
    
    @Override
    protected RankerType getRankerType() {
        return RankerType.RANDOM_FOREST;
    }

    @Override
    protected RankingAggregationMethod getAggregationMethod() {
        return RankingAggregationMethod.EXPONENTIAL_RANKS;
    }

    @Test
    public void executeMitigation() {
        // For meassuring at least 30 runs are required
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.INCREASING;
            createTrainData();
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
    }
    
    @Test
    public void executeBruteForce() throws Exception {
        // For meassuring at least 30 runs are required
        deleteOldMeassurement();
        for (int i = 0; i < MITIGATION_RUNS; i++) {
            var startTime = System.currentTimeMillis();
            mitigationStrategy = MitigationStrategy.BRUTE_FORCE;
            createMitigationCandidatesAutomatically();
            var duration = System.currentTimeMillis() - startTime;
            storeMeassurement(duration);
        }
    }
}