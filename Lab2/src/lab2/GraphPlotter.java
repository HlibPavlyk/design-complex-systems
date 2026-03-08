package lab2;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraphPlotter {

    public static void generateReport(String enabledCsvPath, String disabledCsvPath, String outputPath) {
        List<Integer> sizes = new ArrayList<>();
        List<Double> enabledTimes = new ArrayList<>();
        List<Double> disabledTimes = new ArrayList<>();

        readCSV(enabledCsvPath, sizes, enabledTimes);

        List<Integer> sizes2 = new ArrayList<>();
        readCSV(disabledCsvPath, sizes2, disabledTimes);

        if (sizes.isEmpty() || disabledTimes.isEmpty()) {
            System.err.println("Missing timing data. Ensure both TSX-Enabled and TSX-Disabled benchmarks have been run.");
            return;
        }

        if (sizes.size() != sizes2.size()) {
            System.err.println("Warning: TSX-Enabled and TSX-Disabled have different number of data points.");
        }

        // Print console table
        System.out.println();
        System.out.println("=== TSX-NI Performance Comparison ===");
        System.out.printf(Locale.US, "%-8s | %-16s | %-16s | %-8s%n",
                "N", "TSX-Enabled(ms)", "TSX-Disabled(ms)", "Speedup");
        System.out.println("---------+------------------+------------------+---------");
        int numRows = Math.min(sizes.size(), disabledTimes.size());
        for (int i = 0; i < numRows; i++) {
            double speedup = disabledTimes.get(i) > 0 ? disabledTimes.get(i) / enabledTimes.get(i) : 0;
            System.out.printf(Locale.US, "%-8d | %-16.2f | %-16.2f | %-8.2f%n",
                    sizes.get(i), enabledTimes.get(i), disabledTimes.get(i), speedup);
        }
        System.out.println();

        // Generate HTML report
        String html = generateHTML(sizes, enabledTimes, disabledTimes);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputPath)))) {
            pw.print(html);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write chart: " + outputPath, e);
        }

        System.out.println("Report saved to: " + outputPath);
    }

    private static void readCSV(String path, List<Integer> sizes, List<Double> times) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split(",");
                if (parts.length >= 3) {
                    sizes.add(Integer.parseInt(parts[0]));
                    times.add(Double.parseDouble(parts[2]));
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read CSV: " + path + " (" + e.getMessage() + ")");
        }
    }

    private static String generateHTML(List<Integer> sizes, List<Double> enabledTimes, List<Double> disabledTimes) {
        int numPoints = Math.min(sizes.size(), Math.min(enabledTimes.size(), disabledTimes.size()));

        double maxTime = 0;
        for (int i = 0; i < numPoints; i++) {
            maxTime = Math.max(maxTime, Math.max(enabledTimes.get(i), disabledTimes.get(i)));
        }
        if (maxTime == 0) maxTime = 1;
        maxTime *= 1.1;

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

        // Title
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"30\" text-anchor=\"middle\" font-size=\"16\" font-weight=\"bold\" fill=\"#333\">" +
                "Intel TSX-NI: Lock-based Parallel Performance</text>\n", chartWidth / 2));

        // Y-axis gridlines and labels
        int numYTicks = 5;
        for (int i = 0; i <= numYTicks; i++) {
            double val = maxTime * i / numYTicks;
            double y = marginTop + plotHeight - (plotHeight * i / (double) numYTicks);
            svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%.1f\" x2=\"%d\" y2=\"%.1f\" stroke=\"#e0e0e0\" stroke-width=\"1\"/>\n",
                    marginLeft, y, marginLeft + plotWidth, y));
            svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" font-size=\"11\" fill=\"#666\">%.0f</text>\n",
                    marginLeft - 8, y + 4, val));
        }

        // X-axis labels
        for (int i = 0; i < numPoints; i++) {
            double x = marginLeft + (numPoints > 1 ? (double) i / (numPoints - 1) * plotWidth : plotWidth / 2.0);
            svg.append(String.format(Locale.US, "<line x1=\"%.1f\" y1=\"%d\" x2=\"%.1f\" y2=\"%d\" stroke=\"#e0e0e0\" stroke-width=\"1\" stroke-dasharray=\"4,4\"/>\n",
                    x, marginTop, x, marginTop + plotHeight));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" font-size=\"12\" fill=\"#666\">%d</text>\n",
                    x, marginTop + plotHeight + 20, sizes.get(i)));
        }

        // Axis labels
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\">" +
                "Matrix/Vector Size (N)</text>\n", marginLeft + plotWidth / 2, marginTop + plotHeight + 45));
        svg.append(String.format(Locale.US, "<text x=\"20\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\" " +
                "transform=\"rotate(-90, 20, %d)\">Time (ms)</text>\n",
                marginTop + plotHeight / 2, marginTop + plotHeight / 2));

        // Compute point coordinates
        double[] enX = new double[numPoints], enY = new double[numPoints];
        double[] disX = new double[numPoints], disY = new double[numPoints];

        for (int i = 0; i < numPoints; i++) {
            double x = marginLeft + (numPoints > 1 ? (double) i / (numPoints - 1) * plotWidth : plotWidth / 2.0);
            enX[i] = x;
            enY[i] = marginTop + plotHeight - (enabledTimes.get(i) / maxTime) * plotHeight;
            disX[i] = x;
            disY[i] = marginTop + plotHeight - (disabledTimes.get(i) / maxTime) * plotHeight;
        }

        // Build polyline paths
        StringBuilder enPath = new StringBuilder();
        StringBuilder disPath = new StringBuilder();
        for (int i = 0; i < numPoints; i++) {
            if (i > 0) { enPath.append(" "); disPath.append(" "); }
            enPath.append(String.format(Locale.US, "%.1f,%.1f", enX[i], enY[i]));
            disPath.append(String.format(Locale.US, "%.1f,%.1f", disX[i], disY[i]));
        }

        // Draw lines
        svg.append(String.format(Locale.US, "<polyline points=\"%s\" fill=\"none\" stroke=\"#34A853\" stroke-width=\"3\" stroke-linejoin=\"round\"/>\n", enPath));
        svg.append(String.format(Locale.US, "<polyline points=\"%s\" fill=\"none\" stroke=\"#EA4335\" stroke-width=\"3\" stroke-linejoin=\"round\"/>\n", disPath));

        // Draw data points and value labels
        for (int i = 0; i < numPoints; i++) {
            svg.append(String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"5\" fill=\"#34A853\" stroke=\"white\" stroke-width=\"2\"/>\n", enX[i], enY[i]));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" fill=\"#34A853\" font-weight=\"bold\">%.0f</text>\n",
                    enX[i], enY[i] - 10, enabledTimes.get(i)));

            svg.append(String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"5\" fill=\"#EA4335\" stroke=\"white\" stroke-width=\"2\"/>\n", disX[i], disY[i]));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" fill=\"#EA4335\" font-weight=\"bold\">%.0f</text>\n",
                    disX[i], disY[i] + 18, disabledTimes.get(i)));
        }

        // Axes
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop, marginLeft, marginTop + plotHeight));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop + plotHeight, marginLeft + plotWidth, marginTop + plotHeight));

        // Legend
        int legendX = marginLeft + 15;
        int legendY = marginTop + 10;
        svg.append(String.format(Locale.US, "<rect x=\"%d\" y=\"%d\" width=\"200\" height=\"52\" rx=\"4\" fill=\"white\" stroke=\"#ddd\" stroke-width=\"1\"/>\n", legendX, legendY));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#34A853\" stroke-width=\"3\"/>\n",
                legendX + 10, legendY + 16, legendX + 30, legendY + 16));
        svg.append(String.format(Locale.US, "<circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"#34A853\"/>\n", legendX + 20, legendY + 16));
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#333\">TSX-NI Enabled</text>\n", legendX + 38, legendY + 20));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#EA4335\" stroke-width=\"3\"/>\n",
                legendX + 10, legendY + 38, legendX + 30, legendY + 38));
        svg.append(String.format(Locale.US, "<circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"#EA4335\"/>\n", legendX + 20, legendY + 38));
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#333\">TSX-NI Disabled</text>\n", legendX + 38, legendY + 42));

        svg.append("</svg>\n");

        // Speedup bar chart
        String speedupChart = generateSpeedupChart(sizes, enabledTimes, disabledTimes, numPoints);

        // Wrap in HTML
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Intel TSX-NI: Hardware Lock Elision Benchmark</title>\n");
        html.append("<style>body{font-family:Arial,sans-serif;margin:40px;background:#f5f5f5;} ");
        html.append(".container{background:white;padding:30px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);max-width:950px;margin:0 auto;} ");
        html.append("table{border-collapse:collapse;margin:20px 0;width:100%;} ");
        html.append("th,td{border:1px solid #ddd;padding:8px 12px;text-align:right;} ");
        html.append("th{background:#f0f0f0;} td:first-child,th:first-child{text-align:center;} ");
        html.append(".info{background:#e8f5e9;padding:15px;border-radius:6px;margin:15px 0;} ");
        html.append(".warn{background:#fff3e0;padding:15px;border-radius:6px;margin:15px 0;}</style>\n");
        html.append("</head>\n<body>\n<div class=\"container\">\n");
        html.append("<h1>Intel TSX-NI: Hardware Lock Elision Performance</h1>\n");
        html.append("<p>Variant 15: MD = min(MT)*(MT+MX) - MT*MX; D = (MT+MX)*B - (MT-MX)*E</p>\n");
        html.append("<div class=\"info\"><strong>Synchronization:</strong> ReentrantLock (java.util.concurrent.locks.Lock) ");
        html.append("with fine-grained per-element locking. When TSX-NI is enabled, the CPU can speculatively ");
        html.append("execute lock-protected regions without actually acquiring the lock (Hardware Lock Elision).</div>\n");

        html.append("<h2>Execution Time Comparison</h2>\n");
        html.append(svg);

        html.append("\n<h2>Speedup (TSX-Disabled / TSX-Enabled)</h2>\n");
        html.append(speedupChart);

        // Summary table
        html.append("\n<h2>Timing Data</h2>\n<table>\n");
        html.append("<tr><th>N</th><th>TSX-Enabled (ms)</th><th>TSX-Disabled (ms)</th><th>Speedup</th></tr>\n");
        for (int i = 0; i < numPoints; i++) {
            double speedup = enabledTimes.get(i) > 0 && disabledTimes.get(i) > 0
                    ? disabledTimes.get(i) / enabledTimes.get(i) : 0;
            html.append(String.format(Locale.US, "<tr><td>%d</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>\n",
                    sizes.get(i), enabledTimes.get(i), disabledTimes.get(i), speedup));
        }
        html.append("</table>\n");

        html.append("<h2>Conclusions</h2>\n");
        html.append("<p>When Intel TSX-NI (Transactional Synchronization Extensions) is <strong>enabled</strong>, ");
        html.append("the CPU uses Hardware Lock Elision (HLE) to speculatively execute critical sections ");
        html.append("protected by ReentrantLock without actually acquiring the lock. Since each thread writes ");
        html.append("to non-overlapping regions of the result arrays (MD, D), there are no actual data conflicts, ");
        html.append("and the speculative execution succeeds. This eliminates lock contention overhead.</p>\n");
        html.append("<p>When TSX-NI is <strong>disabled</strong>, every lock acquisition is a real atomic ");
        html.append("compare-and-swap operation, introducing serialization overhead even though threads ");
        html.append("access different memory locations. The fine-grained locking pattern (lock per element write) ");
        html.append("amplifies this effect, as the lock is acquired and released N*N times for MD and N times for D.</p>\n");
        html.append("<p>The speedup from TSX-NI is expected to increase with larger N, as the total number ");
        html.append("of lock operations grows quadratically with matrix size.</p>\n");

        html.append("<h2>TSX-NI Control</h2>\n");
        html.append("<div class=\"warn\">\n");
        html.append("<p><strong>Enable TSX-NI:</strong><br><code>reg add \"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Kernel\" /v DisableTsx /t REG_DWORD /d 0 /f</code></p>\n");
        html.append("<p><strong>Disable TSX-NI:</strong><br><code>reg add \"HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Kernel\" /v DisableTsx /t REG_DWORD /d 1 /f</code></p>\n");
        html.append("<p>Reboot required after each change.</p>\n");
        html.append("</div>\n");

        html.append("</div>\n</body>\n</html>\n");
        return html.toString();
    }

    private static String generateSpeedupChart(List<Integer> sizes, List<Double> enabledTimes,
                                                List<Double> disabledTimes, int numPoints) {
        double[] speedups = new double[numPoints];
        double maxSpeedup = 0;

        for (int i = 0; i < numPoints; i++) {
            speedups[i] = (enabledTimes.get(i) > 0 && disabledTimes.get(i) > 0)
                    ? disabledTimes.get(i) / enabledTimes.get(i) : 0;
            if (speedups[i] > maxSpeedup) maxSpeedup = speedups[i];
        }
        if (maxSpeedup == 0) maxSpeedup = 1;
        maxSpeedup *= 1.15;

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
                "Speedup: TSX-Disabled / TSX-Enabled</text>\n", chartWidth / 2));

        int numYTicks = 5;
        for (int i = 0; i <= numYTicks; i++) {
            double val = maxSpeedup * i / numYTicks;
            double y = marginTop + plotHeight - (plotHeight * i / (double) numYTicks);
            svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%.1f\" x2=\"%d\" y2=\"%.1f\" stroke=\"#e0e0e0\" stroke-width=\"1\"/>\n",
                    marginLeft, y, marginLeft + plotWidth, y));
            svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" font-size=\"11\" fill=\"#666\">%.2f</text>\n",
                    marginLeft - 8, y + 4, val));
        }

        // Reference line at speedup = 1.0
        double refY = marginTop + plotHeight - (1.0 / maxSpeedup) * plotHeight;
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
            double barH = (speedups[i] / maxSpeedup) * plotHeight;
            double barY = marginTop + plotHeight - barH;

            String color = speedups[i] >= 1.0 ? "#34A853" : "#EA4335";

            svg.append(String.format(Locale.US, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\" rx=\"3\"/>\n",
                    cx - barWidth / 2, barY, barWidth, barH, color));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"11\" font-weight=\"bold\" fill=\"#333\">%.2f</text>\n",
                    cx, barY - 5, speedups[i]));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" font-size=\"12\" fill=\"#666\">%d</text>\n",
                    cx, marginTop + plotHeight + 20, sizes.get(i)));
        }

        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\">" +
                "Matrix/Vector Size (N)</text>\n", marginLeft + plotWidth / 2, marginTop + plotHeight + 45));
        svg.append(String.format(Locale.US, "<text x=\"20\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\" " +
                "transform=\"rotate(-90, 20, %d)\">Speedup</text>\n",
                marginTop + plotHeight / 2, marginTop + plotHeight / 2));

        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop, marginLeft, marginTop + plotHeight));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop + plotHeight, marginLeft + plotWidth, marginTop + plotHeight));

        svg.append("</svg>\n");
        return svg.toString();
    }
}
