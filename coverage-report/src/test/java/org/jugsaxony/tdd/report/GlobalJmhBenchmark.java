package org.jugsaxony.tdd.report;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class GlobalJmhBenchmark {

    @Param({"1000"})
    public int size;

    private org.jugsaxony.tdd.demo1.TDDHashMap<String, String> demo1Map;
    private org.jugsaxony.tdd.demo2.TDDHashMap<String, String> demo2Map;
    private org.jugsaxony.tdd.demo3.TDDHashMap<String, String> demo3Map;
    private org.jugsaxony.tdd.demo4.TDDHashMap<String, String> demo4Map;
    private org.jugsaxony.tdd.demo5.TDDHashMap<String, String> demo5Map;
    private org.jugsaxony.tdd.demo6.TDDHashMap<String, String> demo6Map;
    private org.jugsaxony.tdd.demo7.TDDHashMap<String, String> demo7Map;
    private org.jugsaxony.tdd.demo8.TDDHashMap<String, String> demo8Map;
    private org.jugsaxony.tdd.demo9.TDDHashMap<String, String> demo9Map;

    private String[] existingKeys;
    private String[] missingKeys;
    private String[] values;
    private int keyIndex;

    @Setup(Level.Trial)
    public void setup() {
        demo1Map = new org.jugsaxony.tdd.demo1.TDDHashMap<>();
        demo2Map = new org.jugsaxony.tdd.demo2.TDDHashMap<>();
        demo3Map = new org.jugsaxony.tdd.demo3.TDDHashMap<>();
        demo4Map = new org.jugsaxony.tdd.demo4.TDDHashMap<>();
        demo5Map = new org.jugsaxony.tdd.demo5.TDDHashMap<>();
        demo6Map = new org.jugsaxony.tdd.demo6.TDDHashMap<>();
        demo7Map = new org.jugsaxony.tdd.demo7.TDDHashMap<>();
        demo8Map = new org.jugsaxony.tdd.demo8.TDDHashMap<>();
        demo9Map = new org.jugsaxony.tdd.demo9.TDDHashMap<>();

        existingKeys = new String[size];
        missingKeys = new String[size];
        values = new String[size];

        Random rnd = new Random(42);
        for (int i = 0; i < size; i++) {
            existingKeys[i] = "key_" + i + "_" + rnd.nextInt(1_000_000);
            missingKeys[i] = "miss_" + i + "_" + rnd.nextInt(1_000_000);
            values[i] = "val_" + i;

            demo1Map.put(existingKeys[i], values[i]);
            demo2Map.put(existingKeys[i], values[i]);
            demo3Map.put(existingKeys[i], values[i]);
            demo4Map.put(existingKeys[i], values[i]);
            demo5Map.put(existingKeys[i], values[i]);
            demo6Map.put(existingKeys[i], values[i]);
            demo7Map.put(existingKeys[i], values[i]);
            demo8Map.put(existingKeys[i], values[i]);
            demo9Map.put(existingKeys[i], values[i]);
        }
    }

    // ==========================================
    // GET HIT BENCHMARKS
    // ==========================================

    @Benchmark
    public void getHit_demo1(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo1Map.get(existingKeys[idx]));
    }

    @Benchmark
    public void getHit_demo2(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo2Map.get(existingKeys[idx]));
    }

    @Benchmark
    public void getHit_demo3(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo3Map.get(existingKeys[idx]));
    }

    @Benchmark
    public void getHit_demo4(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo4Map.get(existingKeys[idx]));
    }

    @Benchmark
    public void getHit_demo5(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo5Map.get(existingKeys[idx]));
    }

    @Benchmark
    public void getHit_demo6(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo6Map.get(existingKeys[idx]));
    }

    @Benchmark
    public void getHit_demo7(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo7Map.get(existingKeys[idx]));
    }

    @Benchmark
    public void getHit_demo8(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo8Map.get(existingKeys[idx]));
    }

    @Benchmark
    public void getHit_demo9(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo9Map.get(existingKeys[idx]));
    }

    // ==========================================
    // GET MISS BENCHMARKS
    // ==========================================

    @Benchmark
    public void getMiss_demo1(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo1Map.get(missingKeys[idx]));
    }

    @Benchmark
    public void getMiss_demo2(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo2Map.get(missingKeys[idx]));
    }

    @Benchmark
    public void getMiss_demo3(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo3Map.get(missingKeys[idx]));
    }

    @Benchmark
    public void getMiss_demo4(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo4Map.get(missingKeys[idx]));
    }

    @Benchmark
    public void getMiss_demo5(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo5Map.get(missingKeys[idx]));
    }

    @Benchmark
    public void getMiss_demo6(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo6Map.get(missingKeys[idx]));
    }

    @Benchmark
    public void getMiss_demo7(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo7Map.get(missingKeys[idx]));
    }

    @Benchmark
    public void getMiss_demo8(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo8Map.get(missingKeys[idx]));
    }

    @Benchmark
    public void getMiss_demo9(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo9Map.get(missingKeys[idx]));
    }

    // ==========================================
    // PUT / UPDATE BENCHMARKS
    // ==========================================

    @Benchmark
    public void put_demo1(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo1Map.put(existingKeys[idx], values[idx]));
    }

    @Benchmark
    public void put_demo2(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo2Map.put(existingKeys[idx], values[idx]));
    }

    @Benchmark
    public void put_demo3(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo3Map.put(existingKeys[idx], values[idx]));
    }

    @Benchmark
    public void put_demo4(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo4Map.put(existingKeys[idx], values[idx]));
    }

    @Benchmark
    public void put_demo5(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo5Map.put(existingKeys[idx], values[idx]));
    }

    @Benchmark
    public void put_demo6(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo6Map.put(existingKeys[idx], values[idx]));
    }

    @Benchmark
    public void put_demo7(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo7Map.put(existingKeys[idx], values[idx]));
    }

    @Benchmark
    public void put_demo8(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo8Map.put(existingKeys[idx], values[idx]));
    }

    @Benchmark
    public void put_demo9(Blackhole bh) {
        int idx = (keyIndex++) % size;
        if (idx < 0) idx = -idx;
        bh.consume(demo9Map.put(existingKeys[idx], values[idx]));
    }

    public static void fixClasspathForFork() {
        String cp = System.getProperty("java.class.path");
        if (cp == null || !cp.contains("jmh-core")) {
            java.util.Set<String> paths = new java.util.LinkedHashSet<>();
            if (cp != null && !cp.isBlank()) {
                for (String part : cp.split(File.pathSeparator)) {
                    if (!part.isBlank()) paths.add(part);
                }
            }

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            while (cl != null) {
                if (cl instanceof java.net.URLClassLoader ucl) {
                    for (java.net.URL url : ucl.getURLs()) {
                        try {
                            paths.add(new File(url.toURI()).getAbsolutePath());
                        } catch (Exception ignored) {}
                    }
                }
                cl = cl.getParent();
            }

            cl = GlobalJmhBenchmark.class.getClassLoader();
            while (cl != null) {
                if (cl instanceof java.net.URLClassLoader ucl) {
                    for (java.net.URL url : ucl.getURLs()) {
                        try {
                            paths.add(new File(url.toURI()).getAbsolutePath());
                        } catch (Exception ignored) {}
                    }
                }
                cl = cl.getParent();
            }

            File rootDir = GlobalDashboardGenerator.findRootDir();
            File testClasses = new File(rootDir, "coverage-report/target/test-classes");
            if (testClasses.exists()) paths.add(testClasses.getAbsolutePath());
            File classes = new File(rootDir, "coverage-report/target/classes");
            if (classes.exists()) paths.add(classes.getAbsolutePath());

            if (!paths.isEmpty()) {
                System.setProperty("java.class.path", String.join(File.pathSeparator, paths));
            }
        }
    }

    public static boolean isPerfAvailable() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) {
            return false;
        }
        try {
            Process p = new ProcessBuilder("perf", "--version").redirectErrorStream(true).start();
            boolean finished = p.waitFor(2, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        fixClasspathForFork();

        boolean quick = false;
        boolean enablePerf = false;
        boolean enableGc = false;
        String filter = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--quick".equalsIgnoreCase(arg)) {
                quick = true;
            } else if ("--perf".equalsIgnoreCase(arg) || "--perfnorm".equalsIgnoreCase(arg) || "-perf".equalsIgnoreCase(arg)) {
                enablePerf = true;
            } else if ("--gc".equalsIgnoreCase(arg) || "-gc".equalsIgnoreCase(arg)) {
                enableGc = true;
            } else if ("--filter".equalsIgnoreCase(arg) && i + 1 < args.length) {
                filter = args[++i];
            } else if (arg.startsWith("--filter=")) {
                filter = arg.substring("--filter=".length());
            }
        }

        File rootDir = GlobalDashboardGenerator.findRootDir();
        File reportsDir = new File(rootDir, "reports");
        reportsDir.mkdirs();
        File jsonResult = new File(reportsDir, "jmh-results.json");

        OptionsBuilder builder = new OptionsBuilder();
        try {
            builder.parent(new CommandLineOptions(args));
        } catch (CommandLineOptionException ignored) {}

        String targetPattern = (filter != null && !filter.isBlank()) ? filter : GlobalJmhBenchmark.class.getSimpleName();
        builder.include(targetPattern);
        builder.resultFormat(ResultFormatType.JSON);
        builder.result(jsonResult.getAbsolutePath());

        if (quick) {
            builder.warmupIterations(1)
                    .warmupTime(org.openjdk.jmh.runner.options.TimeValue.milliseconds(500))
                    .measurementIterations(1)
                    .measurementTime(org.openjdk.jmh.runner.options.TimeValue.milliseconds(500))
                    .forks(1);
        }

        if (enablePerf) {
            if (isPerfAvailable()) {
                System.out.println("Enabling LinuxPerfNormProfiler for hardware performance counter statistics...");
                builder.addProfiler(org.openjdk.jmh.profile.LinuxPerfNormProfiler.class);
            } else {
                System.err.println("WARNING: --perf requested, but Linux perf is not available. Continuing without perf profiler.");
            }
        }
        if (enableGc) {
            System.out.println("Enabling GCProfiler for memory allocation statistics...");
            builder.addProfiler(org.openjdk.jmh.profile.GCProfiler.class);
        }

        Options opt = builder.build();
        new Runner(opt).run();

        // 1. Generate JMH reports (HTML, Markdown, CSV)
        GlobalJmhReportGenerator.generateReports(jsonResult, reportsDir);

        // 2. Automatically regenerate consolidated dashboards
        try {
            GlobalDashboardGenerator.generateDashboard(reportsDir, rootDir);
            System.out.println("Consolidated master dashboard regenerated successfully.");
        } catch (Exception e) {
            System.err.println("Warning: could not regenerate consolidated dashboard: " + e.getMessage());
        }

        System.out.println("JMH benchmark and hardware counter evaluation completed in: " + reportsDir.getAbsolutePath());
    }
}
