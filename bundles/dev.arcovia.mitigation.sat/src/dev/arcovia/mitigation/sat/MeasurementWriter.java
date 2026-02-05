package dev.arcovia.mitigation.sat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public final class MeasurementWriter implements AutoCloseable {
    private final Path csv;
    private final BufferedWriter out;
    private final Set<String> doneRunIds;
    private final int flushEvery;
    private int linesSinceFlush = 0;

    public MeasurementWriter(Path csv, Set<String> doneRunIds, int flushEvery) throws IOException {
        this.csv = csv;
        this.doneRunIds = doneRunIds;
        this.flushEvery = Math.max(1, flushEvery);

        boolean exists = Files.exists(csv);
        this.out = Files.newBufferedWriter(
                csv,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );

        if (!exists) {
            out.write(String.join(";",
                    "timestamp",
                    "runId",
                    "key",
                    "experiment",
                    "tfgLengthScaling",
                    "tfgAmountScaling",
                    "amountConstraint",
                    "numberWithLabel",
                    "numberWithoutLabel",
                    "numberWithCharacteristic",
                    "numberWithoutCharacteristic",
                    "numberDummyLabels",
                    "runType",
                    "repeatIndex",
                    "executionTime",
                    "solvingTime",
                    "isolatedExecution",
                    "status",
                    "error"
            ));
            out.newLine();
            out.flush();
        }
    }

    public static String runId(RunConfig cfg, String runType, int repeatIndex) {
        return cfg.key() + "|" + runType + "|" + repeatIndex;
    }

    public static Set<String> loadDoneRunIds(Path csv) {
        if (!Files.exists(csv)) return new HashSet<>();
        try {
            List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return new HashSet<>();

            // Column order: timestamp,runId,key,...
            return lines.stream()
                    .skip(1)
                    .map(line -> line.split(",", 3)) // timestamp,runId,key...
                    .filter(parts -> parts.length >= 2)
                    .map(parts -> parts[1])
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
                timer.getExecutionTime(),
                timer.getSolvingTime(),
                timer.getIsolatedExecution(),
                "ok",
                "",
                runId,
                runType,
                repeatIndex);
    }

    public void appendFailure(RunConfig cfg, String runType, int repeatIndex, String error) {
        String runId = runId(cfg, runType, repeatIndex);
        doneRunIds.add(runId);

        writeLine(cfg,
                -1, -1, -1,
                "fail",
                escapeCsv(error),
                runId,
                runType,
                repeatIndex);
    }

    public void writeLine(
            RunConfig cfg,
            long exec,
            long solve,
            long iso,
            String status,
            String error,
            String runId,
            String runType,
            int repeatIndex
    ) {
        try {
            out.write(String.join(";",
                    Instant.now().toString(),
                    runId,
                    cfg.key(),
                    cfg.experiment(),
                    String.valueOf(cfg.tfgLengthScaling()),
                    String.valueOf(cfg.tfgAmountScaling()),
                    String.valueOf(cfg.amountConstraint()),
                    String.valueOf(cfg.numberWithLabel()),
                    String.valueOf(cfg.numberWithoutLabel()),
                    String.valueOf(cfg.numberWithCharacteristic()),
                    String.valueOf(cfg.numberWithoutCharacteristic()),
                    String.valueOf(cfg.numberDummyLabels()),
                    runType,
                    String.valueOf(repeatIndex),
                    String.valueOf(exec),
                    String.valueOf(solve),
                    String.valueOf(iso),
                    status,
                    error
            ));
            out.newLine();

            linesSinceFlush++;
            if (linesSinceFlush >= flushEvery) {
                out.flush();
                linesSinceFlush = 0;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write measurement to CSV: " + csv, e);
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        return s.replace("\n", " ").replace("\r", " ").replace(",", ";");
    }

    @Override
    public void close() throws IOException {
        out.flush();
        out.close();
    }
}

