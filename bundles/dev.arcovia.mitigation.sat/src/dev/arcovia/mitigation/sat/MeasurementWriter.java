package dev.arcovia.mitigation.sat;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;


public final class MeasurementWriter implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path jsonPath;
    private final List<Measurement> records;
    private final Set<String> doneRunIds;
    private final int flushEvery;
    private int linesSinceFlush = 0;

    public MeasurementWriter(Path jsonPath, Set<String> doneRunIds, int flushEvery) throws IOException {
        this.jsonPath = jsonPath;
        this.doneRunIds = doneRunIds;
        this.flushEvery = Math.max(1, flushEvery);

        if (Files.exists(jsonPath)) {
            this.records = MAPPER.readValue(jsonPath.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Measurement.class));
        } else {
            this.records = new ArrayList<>();
        }
    }

    public static String runId(RunConfig cfg, String runType, int repeatIndex) {
        return cfg.key() + "|" + runType + "|" + repeatIndex;
    }

    public static Set<String> loadDoneRunIds(Path jsonPath) {
        if (!Files.exists(jsonPath)) return new HashSet<>();
        try {
            List<Measurement> existing = MAPPER.readValue(jsonPath.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Measurement.class));
            return existing.stream()
                    .map(Measurement::runId)
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (IOException e) {
            return new HashSet<>();
        }
    }

    public boolean isDone(String runId) {
        return doneRunIds.contains(runId);
    }

    public void append(RunConfig cfg, timeMeasurement timer, String runType, int repeatIndex) {
        String runId = runId(cfg, runType, repeatIndex);
        doneRunIds.add(runId);
        writeLine(cfg,
                timer.getExecutionTime(), timer.getSolvingTime(), timer.getIsolatedExecution(),
                "ok", "",
                runId, runType, repeatIndex);
    }

    public void appendFailure(RunConfig cfg, String runType, int repeatIndex, String error) {
        String runId = runId(cfg, runType, repeatIndex);
        doneRunIds.add(runId);
        writeLine(cfg,
                -1, -1, -1,
                "fail", error,
                runId, runType, repeatIndex);
    }

    public void writeLine(
            RunConfig cfg,
            long exec, long solve, long iso,
            String status, String error,
            String runId, String runType, int repeatIndex
    ) {
        records.add(new Measurement(
                Instant.now().toString(),
                runId,
                cfg.key(),
                cfg.experiment(),
                cfg.tfgLengthScaling(),
                cfg.tfgAmountScaling(),
                cfg.amountConstraint(),
                cfg.numberWithLabel(),
                cfg.numberWithoutLabel(),
                cfg.numberWithCharacteristic(),
                cfg.numberWithoutCharacteristic(),
                cfg.numberDummyLabels(),
                runType,
                repeatIndex,
                exec,
                solve,
                iso,
                status,
                error == null ? "" : error
        ));

        linesSinceFlush++;
        if (linesSinceFlush >= flushEvery) {
            flushToDisk();
            linesSinceFlush = 0;
        }
    }

    private void flushToDisk() {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), records);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write measurements to JSON: " + jsonPath, e);
        }
    }

    @Override
    public void close() throws IOException {
        flushToDisk();
    }
}
