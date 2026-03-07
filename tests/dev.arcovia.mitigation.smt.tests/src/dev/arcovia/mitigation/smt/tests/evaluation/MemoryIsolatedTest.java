package dev.arcovia.mitigation.smt.tests.evaluation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.utils.ParsingUtils;

public class MemoryIsolatedTest {

    private static final int RUNS_PER_CONFIGURATION = 100;
    private static final int PARALLELISM = Runtime.getRuntime()
            .availableProcessors();

    @Disabled
    @Test
    public void testAllForMaxProcessMemoryFreshJvmEachRun() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PARALLELISM);
        CompletionService<Long> completion = new ExecutorCompletionService<>(pool);

        List<MemoryResult> memoryResults = new ArrayList<>();
        long pageSizeBytes = readLinuxPageSizeBytes();

        List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();

        try {
            for (EvaluationSupport.Configuration cfg : configs) {
                Config config = new ConfigBuilder().findExpressionTreeSize(true)
                        .build();

                long dagSizeAfter = Mitigation.run(ParsingUtils.loadDFD(cfg.model(), cfg.model() + "_0"), cfg.constraints(), config)
                        .expressionTreeSize()
                        .orElseThrow();

                System.out.println(
                        "Measuring peak RSS in fresh JVM (parallel=" + PARALLELISM + ") for " + cfg.model() + " constraint " + cfg.variantId());

                for (int run = 0; run < RUNS_PER_CONFIGURATION; run++) {
                    final int runIdx = run;
                    completion.submit(() -> {
                        long peak = runInFreshJvmAndMeasurePeakRss(cfg.model(), cfg.variantId(), pageSizeBytes);
                        System.out.println("Completed run " + runIdx + " peakMB=" + (peak / 1024L / 1024L));
                        return peak;
                    });
                }

                List<Long> peakRssBytes = new ArrayList<>(RUNS_PER_CONFIGURATION);
                for (int k = 0; k < RUNS_PER_CONFIGURATION; k++) {
                    Future<Long> f = completion.take();
                    try {
                        peakRssBytes.add(f.get());
                    } catch (ExecutionException ee) {
                        throw new RuntimeException("Measurement task failed", ee.getCause());
                    }
                }

                memoryResults.add(new MemoryResult(dagSizeAfter, peakRssBytes));
            }

            Path out = Path.of("testresults/results/memoryResults/freshJvm" + RUNS_PER_CONFIGURATION + "runs/data.json");
            EvaluationSupport.writeJson(out, memoryResults);

        } finally {
            pool.shutdownNow();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private record MemoryResult(long dagSize, List<Long> peakRssBytes) {
    }

    private static long runInFreshJvmAndMeasurePeakRss(String model, int variantId, long pageSizeBytes) throws Exception {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java")
                .toString();
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classpath, "dev.arcovia.mitigation.smt.tests.evaluation.MemoryIsolatedRunner", model,
                Integer.toString(variantId));

        pb.redirectErrorStream(true);

        Process p = pb.start();
        long pid = p.pid();

        long peakRss = 0;
        while (p.isAlive()) {
            long rss = readRssBytesFromProcStatm(pid, pageSizeBytes);
            if (rss > peakRss)
                peakRss = rss;
            Thread.sleep(1);
        }

        String output;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line)
                        .append('\n');
            output = sb.toString()
                    .trim();
        }

        int exit = p.waitFor();
        if (exit != 0)
            throw new IllegalStateException("Child JVM failed (exit=" + exit + "):\n" + output);

        return peakRss;
    }

    private static long readRssBytesFromProcStatm(long pid, long pageSizeBytes) throws Exception {
        Path path = Path.of("/proc", Long.toString(pid), "statm");
        String statm;
        try {
            statm = Files.readString(path, StandardCharsets.US_ASCII)
                    .trim();
        } catch (NoSuchFileException e) {
            return 0;
        } catch (Exception f) {
            System.out.println(f.getMessage());
            return 0;
        }
        int sp1 = statm.indexOf(' ');
        if (sp1 < 0)
            return 0;

        int sp2 = statm.indexOf(' ', sp1 + 1);
        String residentStr = (sp2 < 0) ? statm.substring(sp1 + 1) : statm.substring(sp1 + 1, sp2);

        long residentPages = Long.parseLong(residentStr);
        return residentPages * pageSizeBytes;
    }

    private static long readLinuxPageSizeBytes() throws Exception {
        Process p = new ProcessBuilder("getconf", "PAGESIZE").start();
        String s;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.US_ASCII))) {
            s = br.readLine();
        }
        p.waitFor();
        return Long.parseLong(s.trim());
    }
}
