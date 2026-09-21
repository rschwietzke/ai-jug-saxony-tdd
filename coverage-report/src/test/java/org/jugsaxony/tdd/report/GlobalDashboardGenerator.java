package org.jugsaxony.tdd.report;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlobalDashboardGenerator {

    public record QualityStats(
            int tests,
            double executionTimeSeconds,
            int failures,
            int errors,
            double instructionCoveragePct,
            double lineCoveragePct,
            double branchCoveragePct,
            int missedInstructions,
            int totalInstructions,
            int missedBranches,
            int totalBranches,
            int missedLines,
            int totalLines,
            int pitKilled,
            int pitTotal,
            double pitScorePct
    ) {}

    public record TddMapSummary(
            String id,
            String name,
            String aiModel,
            String toolchain,
            QualityStats quality,
            long shallowSizeBytes,
            long emptySizeBytes,
            long n1000SizeBytes,
            double n1000BytesPerEntry,
            long n10000ObjectCount,
            double getHitThroughput,
            double getMissThroughput,
            double putThroughput,
            double hitCycles,
            double hitIpc,
            double hitBranchMissRate,
            double hitL1MissRate
    ) {
        public boolean hasPerf() {
            return hitCycles > 0 || hitIpc > 0;
        }
    }

    public static final Map<String, String[]> MODULE_METADATA = Map.of(
            "demo1", new String[]{"Demo 1", "Gemini 3.7 Flash High", "Antigravity Agent (VS Code)"},
            "demo2", new String[]{"Demo 2", "Kimi K3 Max", "Kilo Code (VS Code)"},
            "demo3", new String[]{"Demo 3", "OpenAI 5.6 Sol Max", "Kilo Code (VS Code)"},
            "demo4", new String[]{"Demo 4", "Gemma 4 31B Thinking", "Kilo Code (VS Code)"},
            "demo5", new String[]{"Demo 5", "DeepSeek V4 Flash Max", "Kilo Code (VS Code)"},
            "demo6", new String[]{"Demo 6", "Claude Opus 5 Ultra", "Claude"},
            "demo7", new String[]{"Demo 7", "Qwen 3.8 max XHigh", "Kilo Code (VS Code)"},
            "demo8", new String[]{"Demo 8", "Gemini 3.7 Flash High", "Kilo Code (VS Code)"},
            "demo9", new String[]{"Demo 9", "Gemini 3.8 Flash High", "Antigravity Agent (native)"}
    );

    public static File findRootDir() {
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File pom = new File(current, "pom.xml");
            File demo1 = new File(current, "demo1");
            File demo9 = new File(current, "demo9");
            if (pom.exists() && demo1.isDirectory() && demo9.isDirectory()) {
                try {
                    String content = Files.readString(pom.toPath());
                    if (content.contains("<packaging>pom</packaging>") && content.contains("ai-jug-saxony-tdd-parent")) {
                        return current;
                    }
                } catch (Exception ignored) {}
            }
            current = current.getParentFile();
        }
        return new File(".").getAbsoluteFile();
    }

    public static void generateDashboard(File outputDir, File rootProjectDir) throws Exception {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 1. Gather JMH data if present
        File jmhJson = new File(outputDir, "jmh-results.json");
        if (!jmhJson.exists()) {
            jmhJson = new File(rootProjectDir, "reports/jmh-results.json");
        }

        Map<String, Map<String, Double>> jmhScores = new HashMap<>();
        Map<String, GlobalJmhReportGenerator.BenchmarkEntry> hitEntries = new HashMap<>();
        if (jmhJson.exists()) {
            List<GlobalJmhReportGenerator.BenchmarkEntry> jmhEntries = GlobalJmhReportGenerator.parseJmhJson(jmhJson);
            for (GlobalJmhReportGenerator.BenchmarkEntry entry : jmhEntries) {
                jmhScores.computeIfAbsent(entry.targetId(), k -> new HashMap<>()).put(entry.operation(), entry.score());
                if ("getHit".equals(entry.operation())) {
                    hitEntries.put(entry.targetId(), entry);
                }
            }
        }

        // 2. Gather Submodule Summaries
        List<TddMapSummary> summaries = new ArrayList<>();

        for (GlobalJolReport.ImplementationMeta meta : GlobalJolReport.IMPLEMENTATIONS) {
            String modId = meta.id();
            String[] customMeta = MODULE_METADATA.get(modId);
            String aiModel = customMeta != null ? customMeta[1] : meta.model();
            String toolchain = customMeta != null ? customMeta[2] : "VS Code";

            File modDir = new File(rootProjectDir, modId);
            File surefireDir = new File(modDir, "target/surefire-reports");
            File jacocoXml = new File(outputDir, "jacoco/" + modId + "/jacoco.xml");
            if (!jacocoXml.exists()) {
                jacocoXml = new File(modDir, "target/site/jacoco/jacoco.xml");
            }
            File pitCsv = new File(outputDir, "pit-reports/" + modId + "/mutations.csv");
            if (!pitCsv.exists()) {
                pitCsv = new File(modDir, "target/pit-reports/mutations.csv");
            }

            QualityStats quality = buildQualityStats(surefireDir, jacocoXml, pitCsv, "TDDHashMapTest", "TDDHashMap.java");

            // JOL metrics
            long shallow = 0;
            long empty = 0;
            long n1000 = 0;
            double n1000Bpe = 0.0;
            long n10000Objs = 0;

            try {
                shallow = ClassLayout.parseClass(meta.mapClass()).instanceSize();
                Object emptyMap = meta.factory().create();
                empty = GraphLayout.parseInstance(emptyMap).totalSize();

                Object n1000Map = meta.factory().create();
                for (int k = 0; k < 1000; k++) meta.factory().put(n1000Map, "key_" + k, k);
                n1000 = GraphLayout.parseInstance(n1000Map).totalSize();
                n1000Bpe = (double) n1000 / 1000.0;

                Object n10000Map = meta.factory().create();
                for (int k = 0; k < 10000; k++) meta.factory().put(n10000Map, "key_" + k, k);
                n10000Objs = GraphLayout.parseInstance(n10000Map).totalCount();
            } catch (Throwable ignored) {}

            // JMH
            Map<String, Double> modJmh = jmhScores.getOrDefault(modId, Map.of());
            double hit = modJmh.getOrDefault("getHit", 0.0);
            double miss = modJmh.getOrDefault("getMiss", 0.0);
            double put = modJmh.getOrDefault("put", 0.0);

            GlobalJmhReportGenerator.BenchmarkEntry hitEntry = hitEntries.get(modId);
            double hitCycles = hitEntry != null ? hitEntry.cycles() : 0.0;
            double hitIpc = hitEntry != null ? hitEntry.ipc() : 0.0;
            double hitBranchMiss = hitEntry != null ? hitEntry.branchMissRate() : 0.0;
            double hitL1Miss = hitEntry != null ? hitEntry.l1DcacheMissRate() : 0.0;

            summaries.add(new TddMapSummary(
                    modId,
                    meta.name(),
                    aiModel,
                    toolchain,
                    quality,
                    shallow,
                    empty,
                    n1000,
                    n1000Bpe,
                    n10000Objs,
                    hit,
                    miss,
                    put,
                    hitCycles,
                    hitIpc,
                    hitBranchMiss,
                    hitL1Miss
            ));
        }

        // Copy JaCoCo aggregate report if available
        File jacocoSite = new File(rootProjectDir, "coverage-report/target/site/jacoco-aggregate");
        File destCoverage = new File(outputDir, "coverage-aggregate");
        if (jacocoSite.exists() && jacocoSite.isDirectory()) {
            copyDirectory(jacocoSite, destCoverage);
        }

        // Copy individual module JaCoCo, PIT, and Surefire reports
        for (String modDirName : MODULE_METADATA.keySet()) {
            File modJacoco = new File(rootProjectDir, modDirName + "/target/site/jacoco");
            File destModJacoco = new File(outputDir, "jacoco/" + modDirName);
            if (modJacoco.exists() && modJacoco.isDirectory()) {
                copyDirectory(modJacoco, destModJacoco);
            }

            File modPit = new File(rootProjectDir, modDirName + "/target/pit-reports");
            File destModPit = new File(outputDir, "pit-reports/" + modDirName);
            if (modPit.exists() && modPit.isDirectory()) {
                copyDirectory(modPit, destModPit);
            }

            File modSurefire = new File(rootProjectDir, modDirName + "/target/surefire-reports");
            File destModSurefire = new File(outputDir, "surefire-reports/" + modDirName);
            if (modSurefire.exists() && modSurefire.isDirectory()) {
                copyDirectory(modSurefire, destModSurefire);
            }
        }

        // Write output files
        generateMarkdownDashboard(new File(outputDir, "global-dashboard.md"), summaries);
        generateHtmlDashboard(new File(outputDir, "index.html"), summaries);
        generateTddHashMapHtml(new File(outputDir, "tddhashmap.html"), summaries);
    }

    private static void copyDirectory(File source, File target) throws IOException {
        if (!source.exists()) return;
        if (!target.exists()) target.mkdirs();
        File[] files = source.listFiles();
        if (files == null) return;
        for (File f : files) {
            File destChild = new File(target, f.getName());
            if (f.isDirectory()) {
                copyDirectory(f, destChild);
            } else {
                Files.copy(f.toPath(), destChild.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private record TestExecutionInfo(int tests, double executionTimeSeconds, int failures, int errors) {}

    private static TestExecutionInfo parseTestExecution(File surefireDir, String testPattern) {
        if (!surefireDir.exists() || !surefireDir.isDirectory()) return new TestExecutionInfo(0, 0.0, 0, 0);
        File[] files = surefireDir.listFiles((dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml") && name.contains(testPattern));
        if (files == null || files.length == 0) {
            files = surefireDir.listFiles((dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));
        }
        if (files == null) return new TestExecutionInfo(0, 0.0, 0, 0);

        int totalTests = 0;
        double totalTime = 0.0;
        int totalFailures = 0;
        int totalErrors = 0;

        Pattern suiteTestsPat = Pattern.compile("\\btests=\"([0-9]+)\"");
        Pattern suiteTimePat = Pattern.compile("\\btime=\"([0-9.]+)\"");
        Pattern suiteFailuresPat = Pattern.compile("\\bfailures=\"([0-9]+)\"");
        Pattern suiteErrorsPat = Pattern.compile("\\berrors=\"([0-9]+)\"");

        for (File f : files) {
            try {
                String content = Files.readString(f.toPath());
                Matcher mTests = suiteTestsPat.matcher(content);
                Matcher mTime = suiteTimePat.matcher(content);
                Matcher mFail = suiteFailuresPat.matcher(content);
                Matcher mErr = suiteErrorsPat.matcher(content);

                if (mTests.find()) totalTests += Integer.parseInt(mTests.group(1));
                if (mTime.find()) {
                    try { totalTime += Double.parseDouble(mTime.group(1)); } catch (Exception ignored) {}
                }
                if (mFail.find()) totalFailures += Integer.parseInt(mFail.group(1));
                if (mErr.find()) totalErrors += Integer.parseInt(mErr.group(1));
            } catch (Exception ignored) {}
        }
        return new TestExecutionInfo(totalTests, totalTime, totalFailures, totalErrors);
    }

    private static int[] parseSourceCoverage(File jacocoXml, String sourceFileName, String counterType) {
        if (!jacocoXml.exists()) return new int[]{0, 0};
        try {
            String content = Files.readString(jacocoXml.toPath());
            int sIdx = content.indexOf("<sourcefile name=\"" + sourceFileName + "\"");
            if (sIdx == -1) sIdx = content.indexOf("name=\"" + sourceFileName + "\"");
            if (sIdx == -1) return new int[]{0, 0};

            int endIdx = content.indexOf("</sourcefile>", sIdx);
            if (endIdx == -1) endIdx = content.indexOf("</class>", sIdx);
            if (endIdx == -1) endIdx = content.length();

            String block = content.substring(sIdx, endIdx);
            Pattern p = Pattern.compile("<counter type=\"" + counterType + "\" missed=\"([0-9]+)\" covered=\"([0-9]+)\"/>");
            Matcher m = p.matcher(block);
            if (m.find()) {
                return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
            }
        } catch (Exception ignored) {}
        return new int[]{0, 0};
    }

    private static int[] parsePitCoverage(File pitCsv, String sourceFileName) {
        if (!pitCsv.exists()) return new int[]{0, 0};
        int killed = 0;
        int total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(pitCsv))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(sourceFileName)) {
                    total++;
                    String[] parts = line.split(",");
                    if (parts.length >= 6 && "KILLED".equalsIgnoreCase(parts[5].trim())) {
                        killed++;
                    }
                }
            }
        } catch (Exception ignored) {}
        return new int[]{killed, total};
    }

    private static QualityStats buildQualityStats(File surefireDir, File jacocoXml, File pitCsv, String testPattern, String sourceFileName) {
        TestExecutionInfo testInfo = parseTestExecution(surefireDir, testPattern);
        int[] covInst = parseSourceCoverage(jacocoXml, sourceFileName, "INSTRUCTION");
        int[] covBranch = parseSourceCoverage(jacocoXml, sourceFileName, "BRANCH");
        int[] covLine = parseSourceCoverage(jacocoXml, sourceFileName, "LINE");

        double instPct = covInst[1] + covInst[0] > 0 ? (covInst[1] * 100.0) / (covInst[1] + covInst[0]) : 0.0;
        double branchPct = covBranch[1] + covBranch[0] > 0 ? (covBranch[1] * 100.0) / (covBranch[1] + covBranch[0]) : 0.0;
        double linePct = covLine[1] + covLine[0] > 0 ? (covLine[1] * 100.0) / (covLine[1] + covLine[0]) : 0.0;

        int[] pit = parsePitCoverage(pitCsv, sourceFileName);
        double pitPct = pit[1] > 0 ? (pit[0] * 100.0) / pit[1] : 0.0;

        return new QualityStats(
                testInfo.tests(),
                testInfo.executionTimeSeconds(),
                testInfo.failures(),
                testInfo.errors(),
                instPct,
                linePct,
                branchPct,
                covInst[0],
                covInst[0] + covInst[1],
                covBranch[0],
                covBranch[0] + covBranch[1],
                covLine[0],
                covLine[0] + covLine[1],
                pit[0],
                pit[1],
                pitPct
        );
    }

    private static void generateMarkdownDashboard(File targetFile, List<TddMapSummary> summaries) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(targetFile))) {
            out.println("# AI JUG Saxony TDD — Global Evaluation Dashboard");
            out.println();
            out.println("Comprehensive evaluation of AI coding models implementing an open hashing map (`TDDHashMap`) following a test-driven development workflow.");
            out.println();
            out.println("## 1. Test Verification & Code Quality");
            out.println();
            out.println("| Module | AI Model | Toolchain | Tests Run | Errors/Fails | Duration | JaCoCo Inst Cov | Line Cov | Branch Cov | PIT Mutation Score |");
            out.println("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |");

            for (TddMapSummary s : summaries) {
                QualityStats q = s.quality();
                String errStr = (q.failures() + q.errors() == 0) ? "0" : "**" + (q.failures() + q.errors()) + "** ❌";
                out.printf("| **%s** | %s | %s | %d | %s | %.3fs | %.1f%% | %.1f%% | %.1f%% | %.1f%% (%d/%d) |%n",
                        s.id(),
                        s.aiModel(),
                        s.toolchain(),
                        q.tests(),
                        errStr,
                        q.executionTimeSeconds(),
                        q.instructionCoveragePct(),
                        q.lineCoveragePct(),
                        q.branchCoveragePct(),
                        q.pitScorePct(),
                        q.pitKilled(),
                        q.pitTotal()
                );
            }
            out.println();
            out.println("## 2. Memory Footprint (JOL)");
            out.println();
            out.println("| Module | AI Model | Shallow (B) | Empty (B) | N=1,000 (B) | Bytes/Entry | Objects @ 10k |");
            out.println("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |");
            for (TddMapSummary s : summaries) {
                out.printf("| **%s** | %s | %d B | %,d B | %,d B | %.1f B | %,d |%n",
                        s.id(),
                        s.aiModel(),
                        s.shallowSizeBytes(),
                        s.emptySizeBytes(),
                        s.n1000SizeBytes(),
                        s.n1000BytesPerEntry(),
                        s.n10000ObjectCount()
                );
            }
            out.println();
            out.println("## 3. Microbenchmark Throughput (JMH)");
            out.println();
            out.println("| Module | AI Model | Get Hit (ops/µs) | Get Miss (ops/µs) | Put (ops/µs) | Cycles/op | IPC |");
            out.println("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |");
            for (TddMapSummary s : summaries) {
                String hitStr = s.getHitThroughput() > 0 ? String.format("%.2f", s.getHitThroughput()) : "-";
                String missStr = s.getMissThroughput() > 0 ? String.format("%.2f", s.getMissThroughput()) : "-";
                String putStr = s.putThroughput() > 0 ? String.format("%.2f", s.putThroughput()) : "-";
                String cyclesStr = s.hitCycles() > 0 ? String.format("%.1f", s.hitCycles()) : "-";
                String ipcStr = s.hitIpc() > 0 ? String.format("%.2f", s.hitIpc()) : "-";
                out.printf("| **%s** | %s | %s | %s | %s | %s | %s |%n",
                        s.id(),
                        s.aiModel(),
                        hitStr,
                        missStr,
                        putStr,
                        cyclesStr,
                        ipcStr
                );
            }
        }
    }

    private static void generateHtmlDashboard(File targetFile, List<TddMapSummary> summaries) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(targetFile))) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("    <meta charset=\"UTF-8\">");
            out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            out.println("    <title>AI JUG Saxony TDD — Master Executive Dashboard</title>");
            out.println("    <style>");
            out.println("        :root { --bg: #f8fafc; --card-bg: #ffffff; --text: #0f172a; --text-muted: #64748b; --border: #e2e8f0; --primary: #2563eb; --primary-hover: #1d4ed8; --success: #10b981; --warning: #f59e0b; --danger: #ef4444; }");
            out.println("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: var(--bg); color: var(--text); margin: 0; padding: 2rem; }");
            out.println("        .container { max-width: 1400px; margin: 0 auto; }");
            out.println("        .header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 2px solid var(--border); display: flex; justify-content: space-between; align-items: flex-start; }");
            out.println("        .header-content h1 { margin: 0 0 0.5rem 0; font-size: 2.2rem; color: #1e293b; font-weight: 700; letter-spacing: -0.02em; }");
            out.println("        .header-content p { margin: 0; color: var(--text-muted); font-size: 1.1rem; }");
            out.println("        .nav-tabs { display: flex; gap: 0.5rem; margin-top: 1rem; border-bottom: 1px solid var(--border); padding-bottom: 0.5rem; }");
            out.println("        .nav-tab { padding: 0.5rem 1rem; border-radius: 6px; text-decoration: none; font-weight: 600; font-size: 0.9rem; color: var(--text-muted); background: transparent; transition: all 0.15s; }");
            out.println("        .nav-tab:hover { background: #e2e8f0; color: var(--text); }");
            out.println("        .nav-tab.active { background: var(--primary); color: #ffffff; }");
            out.println("        .grid-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1.25rem; margin-bottom: 2rem; }");
            out.println("        .stat-card { background: var(--card-bg); border-radius: 10px; border: 1px solid var(--border); padding: 1.25rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }");
            out.println("        .stat-card .label { font-size: 0.825rem; text-transform: uppercase; font-weight: 600; letter-spacing: 0.05em; color: var(--text-muted); margin-bottom: 0.25rem; }");
            out.println("        .stat-card .value { font-size: 1.8rem; font-weight: 700; color: #1e293b; }");
            out.println("        .card { background: var(--card-bg); border-radius: 10px; border: 1px solid var(--border); padding: 1.5rem; margin-bottom: 2rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }");
            out.println("        h2 { font-size: 1.3rem; margin-top: 0; margin-bottom: 1rem; color: #1e293b; display: flex; align-items: center; justify-content: space-between; }");
            out.println("        table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }");
            out.println("        th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid var(--border); }");
            out.println("        th { background: #f8fafc; font-weight: 600; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); }");
            out.println("        tr:hover { background: #f8fafc; }");
            out.println("        .badge { display: inline-block; padding: 0.25rem 0.6rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }");
            out.println("        .badge-model { background: #e0f2fe; color: #0369a1; }");
            out.println("        .badge-tool { background: #f3e8ff; color: #7e22ce; }");
            out.println("        .badge-success { background: #dcfce7; color: #15803d; }");
            out.println("        .badge-danger { background: #fee2e2; color: #b91c1c; }");
            out.println("        .progress { background: #e2e8f0; border-radius: 9999px; height: 8px; width: 100px; overflow: hidden; display: inline-block; vertical-align: middle; margin-right: 0.5rem; }");
            out.println("        .progress-bar { height: 100%; border-radius: 9999px; }");
            out.println("        .bar-green { background: #10b981; }");
            out.println("        .bar-yellow { background: #f59e0b; }");
            out.println("        .bar-red { background: #ef4444; }");
            out.println("        .btn { display: inline-block; padding: 0.4rem 0.8rem; border-radius: 6px; font-size: 0.825rem; font-weight: 600; text-decoration: none; background: #f1f5f9; color: #334155; border: 1px solid var(--border); transition: all 0.15s; }");
            out.println("        .btn:hover { background: #e2e8f0; color: #0f172a; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class=\"container\">");

            out.println("    <div class=\"header\">");
            out.println("        <div class=\"header-content\">");
            out.println("            <h1>AI JUG Saxony TDD — Master Dashboard</h1>");
            out.println("            <p>Comparative evaluation of AI coding models implementing an open hashing map (<code>TDDHashMap</code>) via Test-Driven Development.</p>");
            out.println("            <div class=\"nav-tabs\">");
            out.println("                <a href=\"index.html\" class=\"nav-tab active\">Master Dashboard</a>");
            out.println("                <a href=\"tddhashmap.html\" class=\"nav-tab\">TDDHashMap Matrix</a>");
            out.println("                <a href=\"jol-report.html\" class=\"nav-tab\">JOL Memory Footprint</a>");
            out.println("                <a href=\"jmh-report.html\" class=\"nav-tab\">JMH Microbenchmarks</a>");
            out.println("                <a href=\"coverage-aggregate/index.html\" class=\"nav-tab\">Aggregated JaCoCo</a>");
            out.println("            </div>");
            out.println("        </div>");
            out.println("    </div>");

            int totalTests = summaries.stream().mapToInt(s -> s.quality().tests()).sum();
            long greenDemos = summaries.stream().filter(s -> s.quality().failures() + s.quality().errors() == 0 && s.quality().tests() > 0).count();
            double avgInstCov = summaries.stream().mapToDouble(s -> s.quality().instructionCoveragePct()).average().orElse(0.0);
            double avgPit = summaries.stream().mapToDouble(s -> s.quality().pitScorePct()).average().orElse(0.0);

            out.println("    <div class=\"grid-stats\">");
            out.println("        <div class=\"stat-card\"><div class=\"label\">Evaluated Submodules</div><div class=\"value\">" + summaries.size() + " Demos</div></div>");
            out.println("        <div class=\"stat-card\"><div class=\"label\">Passing Test Suites</div><div class=\"value\" style=\"color:#15803d;\">" + greenDemos + " / " + summaries.size() + "</div></div>");
            out.println("        <div class=\"stat-card\"><div class=\"label\">Total Tests Executed</div><div class=\"value\">" + totalTests + "</div></div>");
            out.println("        <div class=\"stat-card\"><div class=\"label\">Avg JaCoCo Instruction Cov</div><div class=\"value\">" + String.format("%.1f%%", avgInstCov) + "</div></div>");
            out.println("        <div class=\"stat-card\"><div class=\"label\">Avg PIT Mutation Score</div><div class=\"value\">" + String.format("%.1f%%", avgPit) + "</div></div>");
            out.println("    </div>");

            out.println("    <div class=\"card\">");
            out.println("        <h2><span>Model Implementations & Quality Overview</span> <a href=\"tddhashmap.html\" class=\"btn\">View Detailed Matrix &rarr;</a></h2>");
            out.println("        <table>");
            out.println("            <thead>");
            out.println("                <tr>");
            out.println("                    <th>Submodule</th>");
            out.println("                    <th>AI Model</th>");
            out.println("                    <th>Toolchain</th>");
            out.println("                    <th>Unit Tests</th>");
            out.println("                    <th>JaCoCo Instruction</th>");
            out.println("                    <th>JaCoCo Branch</th>");
            out.println("                    <th>PIT Mutation Score</th>");
            out.println("                    <th>Empty Mem</th>");
            out.println("                    <th>N=1,000 Mem</th>");
            out.println("                    <th>Get Hit</th>");
            out.println("                    <th>Reports</th>");
            out.println("                </tr>");
            out.println("            </thead>");
            out.println("            <tbody>");

            for (TddMapSummary s : summaries) {
                QualityStats q = s.quality();
                boolean isPass = q.failures() + q.errors() == 0 && q.tests() > 0;
                String statusBadge = isPass
                        ? "<span class=\"badge badge-success\">" + q.tests() + " passed</span>"
                        : "<span class=\"badge badge-danger\">" + (q.failures() + q.errors()) + " errors</span>";

                String instBarColor = q.instructionCoveragePct() >= 90 ? "bar-green" : q.instructionCoveragePct() >= 75 ? "bar-yellow" : "bar-red";
                String branchBarColor = q.branchCoveragePct() >= 80 ? "bar-green" : q.branchCoveragePct() >= 60 ? "bar-yellow" : "bar-red";
                String pitBarColor = q.pitScorePct() >= 80 ? "bar-green" : q.pitScorePct() >= 60 ? "bar-yellow" : "bar-red";

                String hitStr = s.getHitThroughput() > 0 ? String.format("%.1f ops/µs", s.getHitThroughput()) : "-";

                out.println("                <tr>");
                out.printf("                    <td><strong>%s</strong></td>%n", s.id());
                out.printf("                    <td><span class=\"badge badge-model\">%s</span></td>%n", s.aiModel());
                out.printf("                    <td><span class=\"badge badge-tool\">%s</span></td>%n", s.toolchain());
                out.printf("                    <td>%s <span style=\"color:#64748b; font-size:0.8rem;\">(%.2fs)</span></td>%n", statusBadge, q.executionTimeSeconds());
                out.printf("                    <td><div class=\"progress\"><div class=\"progress-bar %s\" style=\"width:%.1f%%;\"></div></div> %.1f%%</td>%n", instBarColor, q.instructionCoveragePct(), q.instructionCoveragePct());
                out.printf("                    <td><div class=\"progress\"><div class=\"progress-bar %s\" style=\"width:%.1f%%;\"></div></div> %.1f%%</td>%n", branchBarColor, q.branchCoveragePct(), q.branchCoveragePct());
                out.printf("                    <td><div class=\"progress\"><div class=\"progress-bar %s\" style=\"width:%.1f%%;\"></div></div> %.1f%% (%d/%d)</td>%n", pitBarColor, q.pitScorePct(), q.pitScorePct(), q.pitKilled(), q.pitTotal());
                out.printf("                    <td>%,d B</td>%n", s.emptySizeBytes());
                out.printf("                    <td>%,d B <span style=\"color:#64748b; font-size:0.8rem;\">(%.1f B/e)</span></td>%n", s.n1000SizeBytes(), s.n1000BytesPerEntry());
                out.printf("                    <td><strong>%s</strong></td>%n", hitStr);
                out.printf("                    <td><a href=\"jacoco/%s/index.html\" class=\"btn\" style=\"padding:0.2rem 0.4rem; font-size:0.75rem;\">JaCoCo</a> <a href=\"pit-reports/%s/index.html\" class=\"btn\" style=\"padding:0.2rem 0.4rem; font-size:0.75rem;\">PIT</a></td>%n", s.id(), s.id());
                out.println("                </tr>");
            }

            out.println("            </tbody>");
            out.println("        </table>");
            out.println("    </div>");

            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    private static void generateTddHashMapHtml(File targetFile, List<TddMapSummary> summaries) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(targetFile))) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("    <meta charset=\"UTF-8\">");
            out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            out.println("    <title>TDDHashMap Implementation Deep-Dive — AI JUG Saxony TDD</title>");
            out.println("    <style>");
            out.println("        :root { --bg: #f8fafc; --card-bg: #ffffff; --text: #0f172a; --text-muted: #64748b; --border: #e2e8f0; --primary: #2563eb; --success: #10b981; }");
            out.println("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: var(--bg); color: var(--text); margin: 0; padding: 2rem; }");
            out.println("        .container { max-width: 1400px; margin: 0 auto; }");
            out.println("        .header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 2px solid var(--border); }");
            out.println("        .nav-link { color: var(--primary); text-decoration: none; font-weight: 500; display: inline-flex; align-items: center; margin-bottom: 1rem; }");
            out.println("        .nav-link:hover { text-decoration: underline; }");
            out.println("        .card { background: var(--card-bg); border-radius: 10px; border: 1px solid var(--border); padding: 1.5rem; margin-bottom: 2rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }");
            out.println("        h1 { margin: 0 0 0.5rem 0; font-size: 2rem; color: #1e293b; }");
            out.println("        h2 { font-size: 1.3rem; margin-top: 0; margin-bottom: 1rem; color: #1e293b; }");
            out.println("        table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }");
            out.println("        th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid var(--border); }");
            out.println("        th { background: #f8fafc; font-weight: 600; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); }");
            out.println("        tr:hover { background: #f8fafc; }");
            out.println("        .badge { display: inline-block; padding: 0.25rem 0.6rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }");
            out.println("        .badge-model { background: #e0f2fe; color: #0369a1; }");
            out.println("        .badge-success { background: #dcfce7; color: #15803d; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class=\"container\">");
            out.println("    <a href=\"index.html\" class=\"nav-link\">&larr; Back to Master Dashboard</a>");
            out.println("    <div class=\"header\">");
            out.println("        <h1>TDDHashMap Cross-Model Quality & Performance Matrix</h1>");
            out.println("        <p>Comprehensive breakdown of open hashing map quality, coverage, memory footprint, and mutation survival rates.</p>");
            out.println("    </div>");

            out.println("    <div class=\"card\">");
            out.println("        <h2>Quality & Mutation Matrix</h2>");
            out.println("        <table>");
            out.println("            <thead>");
            out.println("                <tr>");
            out.println("                    <th>Implementation</th>");
            out.println("                    <th>Model</th>");
            out.println("                    <th>Tests</th>");
            out.println("                    <th>Time</th>");
            out.println("                    <th>Inst Cov</th>");
            out.println("                    <th>Line Cov</th>");
            out.println("                    <th>Branch Cov</th>");
            out.println("                    <th>PIT Killed</th>");
            out.println("                    <th>PIT Total</th>");
            out.println("                    <th>PIT Score</th>");
            out.println("                </tr>");
            out.println("            </thead>");
            out.println("            <tbody>");

            for (TddMapSummary s : summaries) {
                QualityStats q = s.quality();
                out.println("                <tr>");
                out.printf("                    <td><strong>%s</strong></td>%n", s.id());
                out.printf("                    <td><span class=\"badge badge-model\">%s</span></td>%n", s.aiModel());
                out.printf("                    <td>%d</td>%n", q.tests());
                out.printf("                    <td>%.3fs</td>%n", q.executionTimeSeconds());
                out.printf("                    <td>%.1f%%</td>%n", q.instructionCoveragePct());
                out.printf("                    <td>%.1f%%</td>%n", q.lineCoveragePct());
                out.printf("                    <td>%.1f%%</td>%n", q.branchCoveragePct());
                out.printf("                    <td>%d</td>%n", q.pitKilled());
                out.printf("                    <td>%d</td>%n", q.pitTotal());
                out.printf("                    <td><strong>%.1f%%</strong></td>%n", q.pitScorePct());
                out.println("                </tr>");
            }

            out.println("            </tbody>");
            out.println("        </table>");
            out.println("    </div>");

            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    public static void main(String[] args) throws Exception {
        File rootDir = findRootDir();
        File reportsDir = new File(rootDir, "reports");
        reportsDir.mkdirs();
        generateDashboard(reportsDir, rootDir);
        System.out.println("Dashboard generation complete in: " + reportsDir.getAbsolutePath());
    }
}
