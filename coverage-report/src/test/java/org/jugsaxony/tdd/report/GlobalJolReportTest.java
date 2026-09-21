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

        assertThat(dashboardMd).exists().isNotEmpty();
        assertThat(indexHtml).exists().isNotEmpty();
        assertThat(tddMapHtml).exists().isNotEmpty();
    }
}
