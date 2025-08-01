package dev.arcovia.mitigation.ranking.tests.scalability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dataflowanalysis.analysis.core.AbstractVertex;

import dev.arcovia.mitigation.ranking.tests.ScalabilityBase;

public class Koushikkothagal7MitigationTest extends ScalabilityBase {

    protected String getFolderName() {
        return "koushikkothagal";
    }

    protected String getFilesName() {
        return "koushikkothagal";
    }

    protected List<Predicate<? super AbstractVertex<?>>> getConstraints() {
        List<Predicate<? super AbstractVertex<?>>> constraints = new ArrayList<>();
        constraints.add(it -> {
            return this.retrieveNodeLabels(it)
                    .contains("authorization_server")
                    && !this.retrieveNodeLabels(it)
                            .contains("login_attempts_regulation");
        });
        constraints.add(it -> {
            return this.retrieveDataLabels(it)
                    .contains("entrypoint")
                    && !this.retrieveAllDataLabels(it)
                            .contains("encrypted_connection");
        });
        constraints.add(it -> {
            return this.retrieveNodeLabels(it)
                    .contains("internal")
                    && !this.retrieveNodeLabels(it)
                            .contains("local_logging");
        });
        return constraints;
    }
}