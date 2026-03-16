package lab3;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraphPlotter {

    public static void generateReport(String htmCsvPath, String stmCsvPath, String outputPath) {
        List<Integer> htmTotalOps = new ArrayList<>();
        List<Double> htmTimes = new ArrayList<>();
        List<Integer> stmTotalOps = new ArrayList<>();
        List<Double> stmTimes = new ArrayList<>();
        int[] htmThreads = {0};

        readCSV(htmCsvPath, htmTotalOps, htmTimes, htmThreads);
        readCSV(stmCsvPath, stmTotalOps, stmTimes, null);

        if (htmTimes.isEmpty() || stmTimes.isEmpty()) {
            System.err.println("Missing timing data. Ensure both HTM and STM benchmarks have been run.");
            return;
        }

        List<Integer> totalOps = htmTotalOps;
        int numPoints = Math.min(totalOps.size(), Math.min(htmTimes.size(), stmTimes.size()));
        int threads = htmThreads[0];

        // Print console table
        System.out.println();
        System.out.println("=== HTM vs STM Performance Comparison (" + threads + " threads) ===");
        System.out.printf(Locale.US, "%-12s | %-14s | %-14s | %-10s%n",
                "Total Ops", "HTM (ms)", "STM (ms)", "STM/HTM");
        System.out.println("-------------+----------------+----------------+-----------");
        for (int i = 0; i < numPoints; i++) {
            double ratio = htmTimes.get(i) > 0 ? stmTimes.get(i) / htmTimes.get(i) : 0;
            System.out.printf(Locale.US, "%-12d | %-14.2f | %-14.2f | %-10.2f%n",
                    totalOps.get(i), htmTimes.get(i), stmTimes.get(i), ratio);
        }
        System.out.println();

        String html = generateHTML(totalOps, htmTimes, stmTimes, numPoints, threads);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputPath)))) {
            pw.print(html);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write chart: " + outputPath, e);
        }

        System.out.println("Report saved to: " + outputPath);
    }

    /** Reads CSV: threads, iterations_per_thread, total_ops, time_ms */
    private static void readCSV(String path, List<Integer> totalOps, List<Double> times, int[] threadsOut) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split(",");
                if (parts.length >= 4) {
                    if (threadsOut != null) threadsOut[0] = Integer.parseInt(parts[0]);
                    totalOps.add(Integer.parseInt(parts[2]));  // total_ops column
                    times.add(Double.parseDouble(parts[3]));   // time_ms column
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read CSV: " + path + " (" + e.getMessage() + ")");
        }
    }

    private static String generateHTML(List<Integer> totalOps, List<Double> htmTimes,
                                        List<Double> stmTimes, int numPoints, int threads) {
        // --- Main comparison chart ---
        double maxTime = 0;
        for (int i = 0; i < numPoints; i++) {
            maxTime = Math.max(maxTime, Math.max(htmTimes.get(i), stmTimes.get(i)));
        }
        if (maxTime == 0) maxTime = 1;
        maxTime *= 1.15;

        int chartWidth = 850;
        int chartHeight = 450;
        int marginLeft = 90;
        int marginBottom = 60;
        int marginTop = 50;
        int marginRight = 30;
        int plotWidth = chartWidth - marginLeft - marginRight;
        int plotHeight = chartHeight - marginTop - marginBottom;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US, "<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">\n",
                chartWidth, chartHeight));
        svg.append(String.format(Locale.US, "<rect width=\"%d\" height=\"%d\" fill=\"white\"/>\n", chartWidth, chartHeight));

        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"30\" text-anchor=\"middle\" font-size=\"16\" font-weight=\"bold\" fill=\"#333\">" +
                "HTM vs STM: Database Operation Performance (%d threads)</text>\n", chartWidth / 2, threads));

        // Y-axis gridlines
        int numYTicks = 5;
        for (int i = 0; i <= numYTicks; i++) {
            double val = maxTime * i / numYTicks;
            double y = marginTop + plotHeight - (plotHeight * i / (double) numYTicks);
            svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%.1f\" x2=\"%d\" y2=\"%.1f\" stroke=\"#e0e0e0\" stroke-width=\"1\"/>\n",
                    marginLeft, y, marginLeft + plotWidth, y));
            svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" font-size=\"11\" fill=\"#666\">%.0f</text>\n",
                    marginLeft - 8, y + 4, val));
        }

        // X-axis labels (total queries)
        for (int i = 0; i < numPoints; i++) {
            double x = marginLeft + (numPoints > 1 ? (double) i / (numPoints - 1) * plotWidth : plotWidth / 2.0);
            svg.append(String.format(Locale.US, "<line x1=\"%.1f\" y1=\"%d\" x2=\"%.1f\" y2=\"%d\" stroke=\"#e0e0e0\" stroke-width=\"1\" stroke-dasharray=\"4,4\"/>\n",
                    x, marginTop, x, marginTop + plotHeight));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" font-size=\"12\" fill=\"#666\">%d</text>\n",
                    x, marginTop + plotHeight + 20, totalOps.get(i)));
        }

        // Axis labels
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\">" +
                "Total Queries (threads x iterations)</text>\n", marginLeft + plotWidth / 2, marginTop + plotHeight + 45));
        svg.append(String.format(Locale.US, "<text x=\"20\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\" " +
                "transform=\"rotate(-90, 20, %d)\">Time (ms)</text>\n",
                marginTop + plotHeight / 2, marginTop + plotHeight / 2));

        // Compute point coordinates
        double[] htmX = new double[numPoints], htmY = new double[numPoints];
        double[] stmX = new double[numPoints], stmY = new double[numPoints];

        for (int i = 0; i < numPoints; i++) {
            double x = marginLeft + (numPoints > 1 ? (double) i / (numPoints - 1) * plotWidth : plotWidth / 2.0);
            htmX[i] = x;
            htmY[i] = marginTop + plotHeight - (htmTimes.get(i) / maxTime) * plotHeight;
            stmX[i] = x;
            stmY[i] = marginTop + plotHeight - (stmTimes.get(i) / maxTime) * plotHeight;
        }

        // Polylines
        StringBuilder htmPath = new StringBuilder();
        StringBuilder stmPath = new StringBuilder();
        for (int i = 0; i < numPoints; i++) {
            if (i > 0) { htmPath.append(" "); stmPath.append(" "); }
            htmPath.append(String.format(Locale.US, "%.1f,%.1f", htmX[i], htmY[i]));
            stmPath.append(String.format(Locale.US, "%.1f,%.1f", stmX[i], stmY[i]));
        }

        svg.append(String.format(Locale.US, "<polyline points=\"%s\" fill=\"none\" stroke=\"#4285F4\" stroke-width=\"3\" stroke-linejoin=\"round\"/>\n", htmPath));
        svg.append(String.format(Locale.US, "<polyline points=\"%s\" fill=\"none\" stroke=\"#EA4335\" stroke-width=\"3\" stroke-linejoin=\"round\"/>\n", stmPath));

        // Data points + labels
        for (int i = 0; i < numPoints; i++) {
            svg.append(String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"5\" fill=\"#4285F4\" stroke=\"white\" stroke-width=\"2\"/>\n", htmX[i], htmY[i]));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" fill=\"#4285F4\" font-weight=\"bold\">%.0f</text>\n",
                    htmX[i], htmY[i] - 10, htmTimes.get(i)));

            svg.append(String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"5\" fill=\"#EA4335\" stroke=\"white\" stroke-width=\"2\"/>\n", stmX[i], stmY[i]));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" fill=\"#EA4335\" font-weight=\"bold\">%.0f</text>\n",
                    stmX[i], stmY[i] + 18, stmTimes.get(i)));
        }

        // Axes
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop, marginLeft, marginTop + plotHeight));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop + plotHeight, marginLeft + plotWidth, marginTop + plotHeight));

        // Legend
        int legendX = marginLeft + 15;
        int legendY = marginTop + 10;
        svg.append(String.format(Locale.US, "<rect x=\"%d\" y=\"%d\" width=\"260\" height=\"52\" rx=\"4\" fill=\"white\" stroke=\"#ddd\" stroke-width=\"1\"/>\n", legendX, legendY));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#4285F4\" stroke-width=\"3\"/>\n",
                legendX + 10, legendY + 16, legendX + 30, legendY + 16));
        svg.append(String.format(Locale.US, "<circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"#4285F4\"/>\n", legendX + 20, legendY + 16));
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#333\">HTM (ReentrantLock + TSX-NI)</text>\n", legendX + 38, legendY + 20));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#EA4335\" stroke-width=\"3\"/>\n",
                legendX + 10, legendY + 38, legendX + 30, legendY + 38));
        svg.append(String.format(Locale.US, "<circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"#EA4335\"/>\n", legendX + 20, legendY + 38));
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#333\">STM (Multiverse Software TM)</text>\n", legendX + 38, legendY + 42));

        svg.append("</svg>\n");

        // --- Ratio bar chart ---
        String ratioChart = generateRatioChart(totalOps, htmTimes, stmTimes, numPoints, threads);

        // --- HTML ---
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"uk\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Lab 3: HTM vs STM Database Benchmark</title>\n");
        html.append("<style>body{font-family:Arial,sans-serif;margin:40px;background:#f5f5f5;} ");
        html.append(".container{background:white;padding:30px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);max-width:950px;margin:0 auto;} ");
        html.append("table{border-collapse:collapse;margin:20px 0;width:100%;} ");
        html.append("th,td{border:1px solid #ddd;padding:8px 12px;text-align:right;} ");
        html.append("th{background:#f0f0f0;} td:first-child,th:first-child{text-align:center;} ");
        html.append(".info{background:#e3f2fd;padding:15px;border-radius:6px;margin:15px 0;} ");
        html.append(".warn{background:#fff3e0;padding:15px;border-radius:6px;margin:15px 0;} ");
        html.append(".htmbox{background:#e8f5e9;padding:15px;border-radius:6px;margin:15px 0;} ");
        html.append(".stmbox{background:#fce4ec;padding:15px;border-radius:6px;margin:15px 0;}</style>\n");
        html.append("</head>\n<body>\n<div class=\"container\">\n");

        html.append("<h1>Lab 3: Hardware vs Software Transactional Memory</h1>\n");
        html.append("<p><strong>Variant 15</strong> | DBMS: PostgreSQL | API: java.sql (JDBC) | Threads: " + threads + "</p>\n");

        html.append("<div class=\"info\">\n");
        html.append("<strong>Experiment:</strong> " + threads + " threads concurrently execute UPDATE queries on a PostgreSQL database. ");
        html.append("The number of queries per thread varies to produce different total query counts. ");
        html.append("We compare two approaches to managing concurrent access:\n");
        html.append("<ul>\n");
        html.append("<li><strong>HTM</strong> (Hardware Transactional Memory): ReentrantLock with Intel TSX-NI Hardware Lock Elision</li>\n");
        html.append("<li><strong>STM</strong> (Software Transactional Memory): Multiverse library atomic blocks</li>\n");
        html.append("</ul></div>\n");

        html.append("<div class=\"htmbox\">\n");
        html.append("<strong>HTM (Hardware Transactional Memory):</strong> Uses <code>java.util.concurrent.locks.ReentrantLock</code> ");
        html.append("to protect concurrent DB operations. When Intel TSX-NI is enabled, the CPU applies Hardware Lock Elision (HLE): ");
        html.append("lock-protected regions are speculatively executed without actually acquiring the lock. ");
        html.append("Since threads operate on largely non-overlapping records, speculative execution succeeds and lock overhead is eliminated.");
        html.append("</div>\n");

        html.append("<div class=\"stmbox\">\n");
        html.append("<strong>STM (Software Transactional Memory):</strong> Uses the <code>Multiverse</code> library ");
        html.append("(<code>StmUtils.atomic()</code>) to wrap DB operations in software-managed transactional blocks. ");
        html.append("Conflict detection and resolution are managed entirely in software, adding overhead compared to hardware-assisted approach. ");
        html.append("This mode is run with TSX-NI disabled to measure pure STM performance.");
        html.append("</div>\n");

        html.append("<h2>Execution Time Comparison</h2>\n");
        html.append(svg);

        html.append("\n<h2>STM/HTM Ratio (overhead factor)</h2>\n");
        html.append(ratioChart);

        // Summary table
        html.append("\n<h2>Timing Data</h2>\n<table>\n");
        html.append("<tr><th>Total Queries</th><th>Queries/Thread</th><th>HTM (ms)</th><th>STM (ms)</th><th>STM/HTM</th></tr>\n");
        for (int i = 0; i < numPoints; i++) {
            double ratio = htmTimes.get(i) > 0 ? stmTimes.get(i) / htmTimes.get(i) : 0;
            int qPerThread = totalOps.get(i) / threads;
            html.append(String.format(Locale.US, "<tr><td>%d</td><td>%d</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>\n",
                    totalOps.get(i), qPerThread, htmTimes.get(i), stmTimes.get(i), ratio));
        }
        html.append("</table>\n");

        html.append("<h2>Conclusions</h2>\n");
        html.append("<p>When Intel TSX-NI is <strong>enabled</strong>, the CPU uses Hardware Lock Elision (HLE) ");
        html.append("to speculatively execute critical sections protected by <code>ReentrantLock</code> ");
        html.append("without actually acquiring the lock. Hardware transactional memory manages conflicts ");
        html.append("at the CPU level with minimal overhead.</p>\n");

        html.append("<p>Software Transactional Memory (Multiverse) provides the same transactional ");
        html.append("guarantees but with higher overhead due to software-based versioning, conflict ");
        html.append("detection, and potential rollback/retry. Each <code>StmUtils.atomic()</code> ");
        html.append("call adds bookkeeping overhead.</p>\n");

        html.append("<p>The STM/HTM ratio shows how much slower STM is compared to HTM. ");
        html.append("A ratio &gt; 1.0 means STM is slower. The overhead tends to grow with the ");
        html.append("total number of queries as more STM bookkeeping accumulates.</p>\n");

        html.append("<h2>TSX-NI Control (Windows Registry)</h2>\n");
        html.append("<div class=\"warn\">\n");
        html.append("<p><strong>Enable TSX-NI:</strong><br><code>reg add \"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Kernel\" /v DisableTsx /t REG_DWORD /d 0 /f</code></p>\n");
        html.append("<p><strong>Disable TSX-NI:</strong><br><code>reg add \"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Kernel\" /v DisableTsx /t REG_DWORD /d 1 /f</code></p>\n");
        html.append("<p>Reboot required after each change.</p>\n");
        html.append("</div>\n");

        html.append("</div>\n</body>\n</html>\n");
        return html.toString();
    }

    private static String generateRatioChart(List<Integer> totalOps, List<Double> htmTimes,
                                              List<Double> stmTimes, int numPoints, int threads) {
        double[] ratios = new double[numPoints];
        double maxRatio = 0;

        for (int i = 0; i < numPoints; i++) {
            ratios[i] = (htmTimes.get(i) > 0 && stmTimes.get(i) > 0)
                    ? stmTimes.get(i) / htmTimes.get(i) : 0;
            if (ratios[i] > maxRatio) maxRatio = ratios[i];
        }
        if (maxRatio == 0) maxRatio = 1;
        maxRatio *= 1.15;

        int chartWidth = 850;
        int chartHeight = 350;
        int marginLeft = 90;
        int marginBottom = 60;
        int marginTop = 50;
        int marginRight = 30;
        int plotWidth = chartWidth - marginLeft - marginRight;
        int plotHeight = chartHeight - marginTop - marginBottom;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US, "<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">\n",
                chartWidth, chartHeight));
        svg.append(String.format(Locale.US, "<rect width=\"%d\" height=\"%d\" fill=\"white\"/>\n", chartWidth, chartHeight));

        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"30\" text-anchor=\"middle\" font-size=\"16\" font-weight=\"bold\" fill=\"#333\">" +
                "STM / HTM Ratio (%d threads)</text>\n", chartWidth / 2, threads));

        int numYTicks = 5;
        for (int i = 0; i <= numYTicks; i++) {
            double val = maxRatio * i / numYTicks;
            double y = marginTop + plotHeight - (plotHeight * i / (double) numYTicks);
            svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%.1f\" x2=\"%d\" y2=\"%.1f\" stroke=\"#e0e0e0\" stroke-width=\"1\"/>\n",
                    marginLeft, y, marginLeft + plotWidth, y));
            svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" font-size=\"11\" fill=\"#666\">%.2f</text>\n",
                    marginLeft - 8, y + 4, val));
        }

        // Reference line at ratio = 1.0
        double refY = marginTop + plotHeight - (1.0 / maxRatio) * plotHeight;
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%.1f\" x2=\"%d\" y2=\"%.1f\" stroke=\"#999\" stroke-width=\"1\" stroke-dasharray=\"6,3\"/>\n",
                marginLeft, refY, marginLeft + plotWidth, refY));
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%.1f\" font-size=\"10\" fill=\"#999\">1.00 (equal)</text>\n",
                marginLeft + plotWidth + 2, refY + 4));

        int barPadding = 20;
        double totalBarSpace = plotWidth - barPadding * (numPoints + 1);
        double barWidth = totalBarSpace / numPoints;
        if (barWidth > 80) barWidth = 80;

        for (int i = 0; i < numPoints; i++) {
            double cx = marginLeft + barPadding + i * (barWidth + barPadding) + barWidth / 2;
            double barH = (ratios[i] / maxRatio) * plotHeight;
            double barY = marginTop + plotHeight - barH;

            String color = ratios[i] >= 1.0 ? "#EA4335" : "#34A853";

            svg.append(String.format(Locale.US, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\" rx=\"3\"/>\n",
                    cx - barWidth / 2, barY, barWidth, barH, color));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"11\" font-weight=\"bold\" fill=\"#333\">%.2f</text>\n",
                    cx, barY - 5, ratios[i]));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" font-size=\"12\" fill=\"#666\">%d</text>\n",
                    cx, marginTop + plotHeight + 20, totalOps.get(i)));
        }

        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\">" +
                "Total Queries</text>\n", marginLeft + plotWidth / 2, marginTop + plotHeight + 45));
        svg.append(String.format(Locale.US, "<text x=\"20\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\" " +
                "transform=\"rotate(-90, 20, %d)\">Ratio (STM/HTM)</text>\n",
                marginTop + plotHeight / 2, marginTop + plotHeight / 2));

        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop, marginLeft, marginTop + plotHeight));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop + plotHeight, marginLeft + plotWidth, marginTop + plotHeight));

        svg.append("</svg>\n");
        return svg.toString();
    }
}
