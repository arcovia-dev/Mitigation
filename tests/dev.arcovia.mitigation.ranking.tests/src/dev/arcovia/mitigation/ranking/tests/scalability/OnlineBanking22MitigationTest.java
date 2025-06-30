package dev.arcovia.mitigation.ranking.tests.scalability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;

import dev.arcovia.mitigation.ranking.tests.ScalabilityBase;

public class OnlineBanking22MitigationTest extends ScalabilityBase {

    protected String getFolderName() {
        return "OBM_22";
    }

    protected String getFilesName() {
        return "OBM";
    }

    protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
        List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
        constraints.add(it -> {
            boolean vio = this.retrieveNodeLabels(it)
                    .contains("Develop")
                    && this.retrieveDataLabels(it)
                            .contains("Personal");
            return vio;
        });
        constraints.add(it -> {
            boolean vio = this.retrieveNodeLabels(it)
                    .contains("Processable")
                    && this.retrieveDataLabels(it)
                            .contains("Encrypted");
            return vio;
        });
        constraints.add(it -> {
            boolean vio = this.retrieveNodeLabels(it)
                    .contains("nonEU")
                    && this.retrieveDataLabels(it)
                            .contains("Personal");
            return vio;
        });
        return constraints;
    }
}
