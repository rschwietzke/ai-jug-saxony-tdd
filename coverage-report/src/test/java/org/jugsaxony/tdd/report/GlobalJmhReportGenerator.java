package org.jugsaxony.tdd.report;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class GlobalJmhReportGenerator {

    public record BenchmarkEntry(
            String fullBenchmarkName,
            String operation,
            String targetId,
            String modelName,
            double score,
            double scoreError,
            String unit,
            Map<String, Double> secondaryMetrics
    ) {
        public BenchmarkEntry(String fullBenchmarkName, String operation, String targetId, String modelName, double score, double scoreError, String unit) {
            this(fullBenchmarkName, operation, targetId, modelName, score, scoreError, unit, Collections.emptyMap());
        }

        public double getMetric(String name) {
            return secondaryMetrics.getOrDefault(name, 0.0);
        }

        public boolean hasMetric(String name) {
            return secondaryMetrics.containsKey(name);
        }

        public boolean hasPerfMetrics() {
            return hasMetric("cycles") || hasMetric("instructions") || hasMetric("IPC");
        }

        public double cycles() { return getMetric("cycles"); }
        public double instructions() { return getMetric("instructions"); }
        public double ipc() { return getMetric("IPC"); }
        public double cpi() { return getMetric("CPI"); }
        public double branches() { return getMetric("branches"); }
        public double branchMisses() { return getMetric("branch-misses"); }
        public double branchMissRate() {
            double b = branches();
            return b > 0 ? (branchMisses() / b) * 100.0 : 0.0;
        }
        public double l1DcacheLoads() { return getMetric("L1-dcache-loads"); }
        public double l1DcacheMisses() { return getMetric("L1-dcache-load-misses"); }
        public double l1DcacheMissRate() {
            double l = l1DcacheLoads();
            return l > 0 ? (l1DcacheMisses() / l) * 100.0 : 0.0;
        }
        public double l1IcacheLoads() { return getMetric("L1-icache-loads"); }
        public double l1IcacheMisses() { return getMetric("L1-icache-load-misses"); }
        public double stalledCyclesFrontend() { return getMetric("stalled-cycles-frontend"); }
    }

    public static final Map<String, String> MODEL_NAMES = Map.ofEntries(
            Map.entry("demo1", "Demo 1 (Gemini 3.7 Flash High / Antigravity Agent in VSCode)"),
            Map.entry("demo2", "Demo 2 (Kimi K3 Max / Kilo Code)"),
            Map.entry("demo3", "Demo 3 (OpenAI 5.6 Sol Max / Kilo Code)"),
            Map.entry("demo4", "Demo 4 (Gemma 4 31B Thinking / Kilo Code)"),
            Map.entry("demo5", "Demo 5 (DeepSeek V4 Flash Max / Kilo Code)"),
            Map.entry("demo6", "Demo 6 (Claude Opus 5 Ultra / Claude)"),
            Map.entry("demo7", "Demo 7 (Qwen 3.8 max XHigh / Kilo Code)"),
            Map.entry("demo8", "Demo 8 (Gemini 3.7 Flash High / Kilo.Code Agent in VSCode)"),
            Map.entry("demo9", "Demo 9 (Gemini 3.8 Flash High / Antigravity Agent natively)")
    );

    public static List<BenchmarkEntry> parseJmhJson(File jsonFile) throws IOException {
        String content = Files.readString(jsonFile.toPath());
        List<BenchmarkEntry> entries = new ArrayList<>();

        Object parsed;
        try {
            parsed = new MiniJsonParser(content).parse();
        } catch (Exception e) {
            System.err.println("Failed to parse JMH JSON: " + e.getMessage());
            return entries;
        }

        if (!(parsed instanceof List<?> list)) {
            return entries;
        }

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            String benchmark = (String) map.get("benchmark");
            if (benchmark == null) continue;

            double score = 0.0;
            double scoreError = 0.0;
            String unit = "ops/us";

            if (map.get("primaryMetric") instanceof Map<?, ?> pm) {
                if (pm.get("score") instanceof Number n) score = n.doubleValue();
                if (pm.get("scoreError") instanceof Number n) scoreError = n.doubleValue();
                if (pm.get("scoreUnit") instanceof String s) unit = s;
            }

            Map<String, Double> secondary = new LinkedHashMap<>();
            if (map.get("secondaryMetrics") instanceof Map<?, ?> sm) {
                for (Map.Entry<?, ?> entry : sm.entrySet()) {
                    String metricName = String.valueOf(entry.getKey());
                    if (entry.getValue() instanceof Map<?, ?> metricObj) {
                        if (metricObj.get("score") instanceof Number num) {
                            secondary.put(metricName, num.doubleValue());
                        }
                    }
                }
            }

            String methodName = benchmark.substring(benchmark.lastIndexOf('.') + 1);
            String operation = "unknown";
            String targetId = "unknown";

            if (methodName.contains("_")) {
                String[] parts = methodName.split("_", 2);
                operation = parts[0];
                targetId = parts[1];
            }

            String modelName = MODEL_NAMES.getOrDefault(targetId, targetId);
            entries.add(new BenchmarkEntry(benchmark, operation, targetId, modelName, score, scoreError, unit, secondary));
        }

        return entries;
    }

    public static void generateReports(File jsonFile, File outputDir) throws IOException {
        if (!jsonFile.exists()) {
            System.err.println("JMH JSON file not found: " + jsonFile.getAbsolutePath());
            return;
        }

        List<BenchmarkEntry> entries = parseJmhJson(jsonFile);
        if (entries.isEmpty()) {
            System.err.println("No benchmark entries parsed from: " + jsonFile.getAbsolutePath());
            return;
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        generateMarkdownReport(new File(outputDir, "jmh-report.md"), entries);
        generateHtmlReport(new File(outputDir, "jmh-report.html"), entries);
        generateCsvReport(new File(outputDir, "jmh-report.csv"), entries);
    }

    public static void main(String[] args) throws IOException {
        File rootDir = GlobalDashboardGenerator.findRootDir();
        File reportsDir = new File(rootDir, "reports");
        File jsonFile = new File(reportsDir, "jmh-results.json");
        if (jsonFile.exists()) {
            generateReports(jsonFile, reportsDir);
            System.out.println("Regenerated JMH reports in " + reportsDir);
        } else {
            System.err.println("JMH results file not found: " + jsonFile);
        }
    }

    public static void generateMarkdownReport(File targetFile, List<BenchmarkEntry> entries) throws IOException {
        Map<String, List<BenchmarkEntry>> byOp = new LinkedHashMap<>();
        for (BenchmarkEntry e : entries) {
            byOp.computeIfAbsent(e.operation(), k -> new ArrayList<>()).add(e);
        }

        boolean anyPerf = entries.stream().anyMatch(BenchmarkEntry::hasPerfMetrics);

        try (PrintWriter out = new PrintWriter(new FileWriter(targetFile))) {
            out.println("# JMH Microbenchmark Cross-Project Comparison Report");
            out.println();
            out.println("Microbenchmark results comparing all TDD Open Hashing Map implementations (Size = 1,000 items).");
            if (anyPerf) {
                out.println();
                out.println("> ⚡ **Hardware Performance Counters Enabled**: Includes Linux `perf` metrics (Cycles/op, Instructions/op, IPC, Branch Mispredictions, L1 D-Cache Misses).");
            }
            out.println();

            for (Map.Entry<String, List<BenchmarkEntry>> group : byOp.entrySet()) {
                String op = group.getKey();
                List<BenchmarkEntry> list = new ArrayList<>(group.getValue());
                list.sort((a, b) -> Double.compare(b.score(), a.score())); // Highest throughput first

                boolean opHasPerf = list.stream().anyMatch(BenchmarkEntry::hasPerfMetrics);

                out.println("## Operation: `" + op + "`");
                out.println();

                if (opHasPerf) {
                    out.println("| Rank | Implementation | Model | Throughput (ops/µs) | Margin (±) | Cycles/op | Insns/op | IPC | Branch Miss % | L1 D-Cache Miss % |");
                    out.println("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |");

                    int rank = 1;
                    for (BenchmarkEntry e : list) {
                        String cyclesStr = e.cycles() > 0 ? String.format("%.1f", e.cycles()) : "-";
                        String insnsStr = e.instructions() > 0 ? String.format("%.1f", e.instructions()) : "-";
                        String ipcStr = e.ipc() > 0 ? String.format("%.2f", e.ipc()) : "-";
                        String branchMissStr = e.branches() > 0 ? String.format("%.2f%%", e.branchMissRate()) : "-";
                        String l1MissStr = e.l1DcacheLoads() > 0 ? String.format("%.2f%%", e.l1DcacheMissRate()) : "-";

                        out.printf("| %d | **%s** | %s | %,.2f | ± %,.2f | %s | %s | %s | %s | %s |%n",
                                rank++,
                                e.targetId(),
                                e.modelName(),
                                e.score(),
                                e.scoreError(),
                                cyclesStr,
                                insnsStr,
                                ipcStr,
                                branchMissStr,
                                l1MissStr
                        );
                    }
                } else {
                    out.println("| Rank | Implementation | Model | Throughput (ops/µs) | Margin (±) |");
                    out.println("| :--- | :--- | :--- | :--- | :--- |");

                    int rank = 1;
                    for (BenchmarkEntry e : list) {
                        out.printf("| %d | **%s** | %s | %,.2f | ± %,.2f |%n",
                                rank++,
                                e.targetId(),
                                e.modelName(),
                                e.score(),
                                e.scoreError()
                        );
                    }
                }
                out.println();
            }
        }
    }

    public static void generateCsvReport(File targetFile, List<BenchmarkEntry> entries) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(targetFile))) {
            out.println("Operation,TargetId,ModelName,ThroughputOpsPerUs,ScoreError,CyclesPerOp,InstructionsPerOp,IPC,CPI,BranchMissRatePct,L1DcacheMissRatePct");
            for (BenchmarkEntry e : entries) {
                out.printf(Locale.US, "\"%s\",\"%s\",\"%s\",%.4f,%.4f,%.2f,%.2f,%.4f,%.4f,%.4f,%.4f%n",
                        e.operation(),
                        e.targetId(),
                        e.modelName(),
                        e.score(),
                        e.scoreError(),
                        e.cycles(),
                        e.instructions(),
                        e.ipc(),
                        e.cpi(),
                        e.branchMissRate(),
                        e.l1DcacheMissRate()
                );
            }
        }
    }

    public static void generateHtmlReport(File targetFile, List<BenchmarkEntry> entries) throws IOException {
        Map<String, List<BenchmarkEntry>> byOp = new LinkedHashMap<>();
        for (BenchmarkEntry e : entries) {
            byOp.computeIfAbsent(e.operation(), k -> new ArrayList<>()).add(e);
        }

        boolean anyPerf = entries.stream().anyMatch(BenchmarkEntry::hasPerfMetrics);

        try (PrintWriter out = new PrintWriter(new FileWriter(targetFile))) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("    <meta charset=\"UTF-8\">");
            out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            out.println("    <title>JMH Microbenchmark Cross-Project Comparison — AI JUG Saxony TDD</title>");
            out.println("    <style>");
            out.println("        :root { --bg: #f8fafc; --card-bg: #ffffff; --text: #0f172a; --text-muted: #64748b; --border: #e2e8f0; --primary: #3b82f6; --success: #10b981; --accent: #8b5cf6; }");
            out.println("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: var(--bg); color: var(--text); margin: 0; padding: 2rem; }");
            out.println("        .container { max-width: 1300px; margin: 0 auto; }");
            out.println("        .header { margin-bottom: 2rem; padding-bottom: 1rem; border-bottom: 2px solid var(--border); }");
            out.println("        .header h1 { margin: 0 0 0.5rem 0; font-size: 2rem; color: #1e293b; }");
            out.println("        .header p { margin: 0; color: var(--text-muted); font-size: 1.1rem; }");
            out.println("        .card { background: var(--card-bg); border-radius: 10px; border: 1px solid var(--border); padding: 1.5rem; margin-bottom: 2rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }");
            out.println("        h2 { font-size: 1.4rem; margin-top: 0; margin-bottom: 1rem; color: #1e293b; display: flex; align-items: center; gap: 0.5rem; }");
            out.println("        table { width: 100%; border-collapse: collapse; margin-top: 1rem; }");
            out.println("        th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid var(--border); }");
            out.println("        th { background: #f1f5f9; font-weight: 600; font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); }");
            out.println("        tr:hover { background: #f8fafc; }");
            out.println("        .badge { display: inline-block; padding: 0.25rem 0.5rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }");
            out.println("        .badge-primary { background: #dbeafe; color: #1d4ed8; }");
            out.println("        .badge-perf { background: #fef3c7; color: #92400e; font-weight: bold; }");
            out.println("        .nav-link { color: var(--primary); text-decoration: none; font-weight: 500; display: inline-flex; align-items: center; margin-bottom: 1rem; }");
            out.println("        .nav-link:hover { text-decoration: underline; }");
            out.println("        .bar-container { background: #e2e8f0; border-radius: 4px; height: 12px; width: 100%; max-width: 140px; overflow: hidden; display: inline-block; vertical-align: middle; margin-left: 0.5rem; }");
            out.println("        .bar-fill { background: var(--primary); height: 100%; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class=\"container\">");
            out.println("    <a href=\"index.html\" class=\"nav-link\">&larr; Back to Master Dashboard</a>");
            out.println("    <div class=\"header\">");
            out.println("        <h1>JMH Microbenchmark Cross-Project Comparison</h1>");
            out.println("        <p>Throughput and micro-architectural CPU performance analysis comparing AI model implementations (Size = 1,000 items).</p>");
            if (anyPerf) {
                out.println("        <p><span class=\"badge badge-perf\">⚡ Hardware Perf Counters Enabled</span> Linux <code>perf</code> hardware counters captured per operation.</p>");
            }
            out.println("    </div>");

            for (Map.Entry<String, List<BenchmarkEntry>> group : byOp.entrySet()) {
                String op = group.getKey();
                List<BenchmarkEntry> list = new ArrayList<>(group.getValue());
                list.sort((a, b) -> Double.compare(b.score(), a.score()));

                double maxScore = list.stream().mapToDouble(BenchmarkEntry::score).max().orElse(1.0);
                boolean opHasPerf = list.stream().anyMatch(BenchmarkEntry::hasPerfMetrics);

                out.println("    <div class=\"card\">");
                out.printf("        <h2>Operation: <code>%s</code></h2>%n", op);
                out.println("        <table>");
                out.println("            <thead>");
                out.println("                <tr>");
                out.println("                    <th>Rank</th>");
                out.println("                    <th>Implementation</th>");
                out.println("                    <th>AI Model</th>");
                out.println("                    <th>Throughput (ops/µs)</th>");
                out.println("                    <th>Margin</th>");
                if (opHasPerf) {
                    out.println("                    <th>Cycles/op</th>");
                    out.println("                    <th>Insns/op</th>");
                    out.println("                    <th>IPC</th>");
                    out.println("                    <th>Branch Miss %</th>");
                    out.println("                    <th>L1 D-Cache Miss %</th>");
                }
                out.println("                </tr>");
                out.println("            </thead>");
                out.println("            <tbody>");

                int rank = 1;
                for (BenchmarkEntry e : list) {
                    double pctOfMax = maxScore > 0 ? (e.score() / maxScore) * 100.0 : 0.0;
                    out.println("                <tr>");
                    out.printf("                    <td>%d</td>%n", rank++);
                    out.printf("                    <td><strong>%s</strong></td>%n", e.targetId());
                    out.printf("                    <td><span class=\"badge badge-primary\">%s</span></td>%n", e.modelName());
                    out.printf("                    <td><strong>%,.2f</strong> <div class=\"bar-container\"><div class=\"bar-fill\" style=\"width: %.1f%%;\"></div></div></td>%n", e.score(), pctOfMax);
                    out.printf("                    <td>± %,.2f</td>%n", e.scoreError());

                    if (opHasPerf) {
                        String cyclesStr = e.cycles() > 0 ? String.format("%.1f", e.cycles()) : "-";
                        String insnsStr = e.instructions() > 0 ? String.format("%.1f", e.instructions()) : "-";
                        String ipcStr = e.ipc() > 0 ? String.format("%.2f", e.ipc()) : "-";
                        String branchMissStr = e.branches() > 0 ? String.format("%.2f%%", e.branchMissRate()) : "-";
                        String l1MissStr = e.l1DcacheLoads() > 0 ? String.format("%.2f%%", e.l1DcacheMissRate()) : "-";

                        out.printf("                    <td>%s</td>%n", cyclesStr);
                        out.printf("                    <td>%s</td>%n", insnsStr);
                        out.printf("                    <td>%s</td>%n", ipcStr);
                        out.printf("                    <td>%s</td>%n", branchMissStr);
                        out.printf("                    <td>%s</td>%n", l1MissStr);
                    }
                    out.println("                </tr>");
                }

                out.println("            </tbody>");
                out.println("        </table>");
                out.println("    </div>");
            }

            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    public static class MiniJsonParser {
        private final String src;
        private int pos = 0;

        public MiniJsonParser(String src) {
            this.src = src;
        }

        public Object parse() {
            skipWhitespace();
            Object val = parseValue();
            skipWhitespace();
            return val;
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // '{'
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ':') {
                    pos++;
                }
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else if (pos < src.length() && src.charAt(pos) == '}') {
                    pos++;
                    break;
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // '['
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < src.length()) {
                Object val = parseValue();
                list.add(val);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else if (pos < src.length() && src.charAt(pos) == ']') {
                    pos++;
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            pos++; // opening '"'
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    if (pos < src.length()) {
                        char esc = src.charAt(pos++);
                        if (esc == 'n') sb.append('\n');
                        else if (esc == 'r') sb.append('\r');
                        else if (esc == 't') sb.append('\t');
                        else if (esc == 'b') sb.append('\b');
                        else if (esc == 'f') sb.append('\f');
                        else sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) { pos += 4; return true; }
            if (src.startsWith("false", pos)) { pos += 5; return false; }
            return false;
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) { pos += 4; }
            return null;
        }

        private Number parseNumber() {
            int start = pos;
            if (pos < src.length() && (src.charAt(pos) == '-' || src.charAt(pos) == '+')) {
                pos++;
            }
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    pos++;
                } else {
                    break;
                }
            }
            String s = src.substring(start, pos);
            try {
                if (s.contains(".") || s.contains("e") || s.contains("E")) {
                    return Double.parseDouble(s);
                } else {
                    return Long.parseLong(s);
                }
            } catch (Exception e) {
                return 0.0;
            }
        }
    }
}
