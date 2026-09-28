package org.jugsaxony.tdd.report;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalJolReportTest {

    @Test
    public void testGenerateJolAndDashboardReports() throws Exception {
        File rootDir = GlobalDashboardGenerator.findRootDir();
        File outputDir = new File(rootDir, "reports");
        outputDir.mkdirs();

        // 1. Generate JOL Report
        GlobalJolReport.generateReports(outputDir);

        File mdReport = new File(outputDir, "jol-report.md");
        File htmlReport = new File(outputDir, "jol-report.html");

        assertThat(mdReport).exists().isNotEmpty();
        assertThat(htmlReport).exists().isNotEmpty();

        // 2. Generate Master Dashboard & TDDHashMap Matrix
        GlobalDashboardGenerator.generateDashboard(outputDir, rootDir);

        File dashboardMd = new File(outputDir, "global-dashboard.md");
        File indexHtml = new File(outputDir, "index.html");
        File tddMapHtml = new File(outputDir, "tddhashmap.html");
        File surefireHtml = new File(outputDir, "surefire.html");

        assertThat(dashboardMd).exists().isNotEmpty();
        assertThat(indexHtml).exists().isNotEmpty();
        assertThat(tddMapHtml).exists().isNotEmpty();
        assertThat(surefireHtml).exists().isNotEmpty();

        // 3. Generate JMH reports if jmh-results.json is present
        File jmhJson = new File(outputDir, "jmh-results.json");
        if (jmhJson.exists()) {
            GlobalJmhReportGenerator.generateReports(jmhJson, outputDir);
            File jmhHtml = new File(outputDir, "jmh-report.html");
            File jmhMd = new File(outputDir, "jmh-report.md");
            assertThat(jmhHtml).exists().isNotEmpty();
            assertThat(jmhMd).exists().isNotEmpty();
        }

        // 4. Assert source code viewer and raw source snapshots are generated
        File demo1Html = new File(outputDir, "sources/demo1/TDDHashMap.html");
        File demo1Java = new File(outputDir, "sources/demo1/TDDHashMap.java");
        File demo1TestHtml = new File(outputDir, "sources/demo1/TDDHashMapTest.html");
        File demo1TestJava = new File(outputDir, "sources/demo1/TDDHashMapTest.java");
        assertThat(demo1Html).exists().isNotEmpty();
        assertThat(demo1Java).exists().isNotEmpty();
        assertThat(demo1TestHtml).exists().isNotEmpty();
        assertThat(demo1TestJava).exists().isNotEmpty();
    }
}
