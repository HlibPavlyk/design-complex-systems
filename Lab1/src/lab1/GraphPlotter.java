package lab1;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraphPlotter {

    public static void generateReport(String csvPath, String outputPath) {
        List<Integer> sizes = new ArrayList<>();
        List<Double> localTimes = new ArrayList<>();
        List<Double> sharedTimes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split(",");
                if (parts.length >= 4) {
                    sizes.add(Integer.parseInt(parts[0]));
                    localTimes.add(Double.parseDouble(parts[2]));
                    sharedTimes.add(Double.parseDouble(parts[3]));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV: " + csvPath, e);
        }

        if (sizes.isEmpty()) {
            System.err.println("No timing data found in " + csvPath);
            return;
        }

        // Print console table
        System.out.println();
        System.out.println("=== Performance Comparison ===");
        System.out.printf(Locale.US, "%-8s | %-12s | %-12s | %-8s%n", "N", "Local(ms)", "Shared(ms)", "Ratio");
        System.out.println("---------+--------------+--------------+---------");
        for (int i = 0; i < sizes.size(); i++) {
            double ratio = localTimes.get(i) > 0 ? sharedTimes.get(i) / localTimes.get(i) : 0;
            System.out.printf(Locale.US, "%-8d | %-12.2f | %-12.2f | %-8.2f%n",
                    sizes.get(i), localTimes.get(i), sharedTimes.get(i), ratio);
        }
        System.out.println();

        // Generate HTML with SVG chart
        String html = generateHTML(sizes, localTimes, sharedTimes);

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputPath)))) {
            pw.print(html);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write chart: " + outputPath, e);
        }

        System.out.println("Chart saved to: " + outputPath);
    }

    private static String generateHTML(List<Integer> sizes, List<Double> localTimes, List<Double> sharedTimes) {
        double maxTime = 0;
        for (int i = 0; i < sizes.size(); i++) {
            maxTime = Math.max(maxTime, Math.max(localTimes.get(i), sharedTimes.get(i)));
        }
        if (maxTime == 0) maxTime = 1;
        maxTime *= 1.1; // 10% padding on top

        int maxN = 0;
        for (int s : sizes) if (s > maxN) maxN = s;

        int chartWidth = 850;
        int chartHeight = 450;
        int marginLeft = 90;
        int marginBottom = 60;
        int marginTop = 50;
        int marginRight = 30;
        int plotWidth = chartWidth - marginLeft - marginRight;
        int plotHeight = chartHeight - marginTop - marginBottom;

        int numPoints = sizes.size();

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US, "<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">\n",
                chartWidth, chartHeight));

        // Background
        svg.append(String.format(Locale.US, "<rect width=\"%d\" height=\"%d\" fill=\"white\"/>\n", chartWidth, chartHeight));

        // Title
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"30\" text-anchor=\"middle\" font-size=\"16\" font-weight=\"bold\" fill=\"#333\">" +
                "MESIF Cache Coherence: Local vs Shared Performance</text>\n", chartWidth / 2));

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

        // X-axis tick marks and labels
        for (int i = 0; i < numPoints; i++) {
            double x = marginLeft + (double) i / (numPoints - 1) * plotWidth;
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
        double[] localX = new double[numPoints];
        double[] localY = new double[numPoints];
        double[] sharedX = new double[numPoints];
        double[] sharedY = new double[numPoints];

        for (int i = 0; i < numPoints; i++) {
            double x = marginLeft + (double) i / (numPoints - 1) * plotWidth;
            localX[i] = x;
            localY[i] = marginTop + plotHeight - (localTimes.get(i) / maxTime) * plotHeight;
            sharedX[i] = x;
            sharedY[i] = marginTop + plotHeight - (sharedTimes.get(i) / maxTime) * plotHeight;
        }

        // Build polyline paths
        StringBuilder localPath = new StringBuilder();
        StringBuilder sharedPath = new StringBuilder();
        for (int i = 0; i < numPoints; i++) {
            if (i > 0) {
                localPath.append(" ");
                sharedPath.append(" ");
            }
            localPath.append(String.format(Locale.US, "%.1f,%.1f", localX[i], localY[i]));
            sharedPath.append(String.format(Locale.US, "%.1f,%.1f", sharedX[i], sharedY[i]));
        }

        // Draw lines
        svg.append(String.format(Locale.US, "<polyline points=\"%s\" fill=\"none\" stroke=\"#4285F4\" stroke-width=\"3\" stroke-linejoin=\"round\"/>\n",
                localPath));
        svg.append(String.format(Locale.US, "<polyline points=\"%s\" fill=\"none\" stroke=\"#EA4335\" stroke-width=\"3\" stroke-linejoin=\"round\"/>\n",
                sharedPath));

        // Draw data points and value labels
        for (int i = 0; i < numPoints; i++) {
            // Local points (blue)
            svg.append(String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"5\" fill=\"#4285F4\" stroke=\"white\" stroke-width=\"2\"/>\n",
                    localX[i], localY[i]));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" fill=\"#4285F4\" font-weight=\"bold\">%.0f</text>\n",
                    localX[i], localY[i] - 10, localTimes.get(i)));

            // Shared points (red)
            svg.append(String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"5\" fill=\"#EA4335\" stroke=\"white\" stroke-width=\"2\"/>\n",
                    sharedX[i], sharedY[i]));
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" fill=\"#EA4335\" font-weight=\"bold\">%.0f</text>\n",
                    sharedX[i], sharedY[i] + 18, sharedTimes.get(i)));
        }

        // Axes
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop, marginLeft, marginTop + plotHeight));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop + plotHeight, marginLeft + plotWidth, marginTop + plotHeight));

        // Legend
        int legendX = marginLeft + 15;
        int legendY = marginTop + 10;
        svg.append(String.format(Locale.US, "<rect x=\"%d\" y=\"%d\" width=\"180\" height=\"52\" rx=\"4\" fill=\"white\" stroke=\"#ddd\" stroke-width=\"1\"/>\n",
                legendX, legendY));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#4285F4\" stroke-width=\"3\"/>\n",
                legendX + 10, legendY + 16, legendX + 30, legendY + 16));
        svg.append(String.format(Locale.US, "<circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"#4285F4\"/>\n",
                legendX + 20, legendY + 16));
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#333\">Local (no MESIF)</text>\n",
                legendX + 38, legendY + 20));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#EA4335\" stroke-width=\"3\"/>\n",
                legendX + 10, legendY + 38, legendX + 30, legendY + 38));
        svg.append(String.format(Locale.US, "<circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"#EA4335\"/>\n",
                legendX + 20, legendY + 38));
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" font-size=\"12\" fill=\"#333\">Shared (MESIF)</text>\n",
                legendX + 38, legendY + 42));

        svg.append("</svg>\n");

        // Wrap in HTML
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>MESIF Cache Coherence Benchmark</title>\n");
        html.append("<style>body{font-family:Arial,sans-serif;margin:40px;background:#f5f5f5;} ");
        html.append(".container{background:white;padding:30px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);max-width:950px;margin:0 auto;} ");
        html.append("table{border-collapse:collapse;margin:20px 0;width:100%;} ");
        html.append("th,td{border:1px solid #ddd;padding:8px 12px;text-align:right;} ");
        html.append("th{background:#f0f0f0;} td:first-child,th:first-child{text-align:center;}</style>\n");
        html.append("</head>\n<body>\n<div class=\"container\">\n");
        html.append("<h1>MESIF Cache Coherence Protocol: Performance Comparison</h1>\n");
        html.append("<p>Variant 15: MD = min(MT)*(MT+MX) - MT*MX; D = (MT+MX)*B - (MT-MX)*E</p>\n");
        html.append(svg);

        // Speedup coefficient bar chart
        html.append("\n<h2>Speedup Coefficient (Local / Shared)</h2>\n");
        html.append(generateSpeedupChart(sizes, localTimes, sharedTimes));

        // Table
        html.append("\n<h2>Timing Data</h2>\n<table>\n");
        html.append("<tr><th>N</th><th>Local (ms)</th><th>Shared (ms)</th><th>Speedup (Local/Shared)</th></tr>\n");
        for (int i = 0; i < sizes.size(); i++) {
            double speedup = localTimes.get(i) > 0 && sharedTimes.get(i) > 0
                    ? localTimes.get(i) / sharedTimes.get(i) : 0;
            html.append(String.format(Locale.US, "<tr><td>%d</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>\n",
                    sizes.get(i), localTimes.get(i), sharedTimes.get(i), speedup));
        }
        html.append("</table>\n");

        html.append("<h2>Conclusions</h2>\n");
        html.append("<p>The Shared version benefits from the MESIF cache coherence protocol, which allows CPU cores ");
        html.append("to fetch shared data directly from other cores' L1D caches (Forward state) instead of main memory. ");
        html.append("As matrix size N grows, the Local version suffers from deep-copy overhead and increased cache pressure, ");
        html.append("making the Shared version progressively faster.</p>\n");

        html.append("</div>\n</body>\n</html>\n");
        return html.toString();
    }

    private static String generateSpeedupChart(List<Integer> sizes, List<Double> localTimes, List<Double> sharedTimes) {
        int numPoints = sizes.size();
        double[] speedups = new double[numPoints];
        double maxSpeedup = 0;

        for (int i = 0; i < numPoints; i++) {
            speedups[i] = (localTimes.get(i) > 0 && sharedTimes.get(i) > 0)
                    ? localTimes.get(i) / sharedTimes.get(i) : 0;
            if (speedups[i] > maxSpeedup) maxSpeedup = speedups[i];
        }
        if (maxSpeedup == 0) maxSpeedup = 1;
        maxSpeedup *= 1.15; // 15% padding

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

        // Title
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"30\" text-anchor=\"middle\" font-size=\"16\" font-weight=\"bold\" fill=\"#333\">" +
                "Speedup Coefficient (Local / Shared)</text>\n", chartWidth / 2));

        // Y-axis gridlines and labels
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

        // Bars
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

            // Value label on top of bar
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"11\" font-weight=\"bold\" fill=\"#333\">%.2f</text>\n",
                    cx, barY - 5, speedups[i]));

            // X-axis label
            svg.append(String.format(Locale.US, "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\" font-size=\"12\" fill=\"#666\">%d</text>\n",
                    cx, marginTop + plotHeight + 20, sizes.get(i)));
        }

        // Axis labels
        svg.append(String.format(Locale.US, "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\">" +
                "Matrix/Vector Size (N)</text>\n", marginLeft + plotWidth / 2, marginTop + plotHeight + 45));
        svg.append(String.format(Locale.US, "<text x=\"20\" y=\"%d\" text-anchor=\"middle\" font-size=\"13\" fill=\"#444\" " +
                "transform=\"rotate(-90, 20, %d)\">Speedup</text>\n",
                marginTop + plotHeight / 2, marginTop + plotHeight / 2));

        // Axes
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop, marginLeft, marginTop + plotHeight));
        svg.append(String.format(Locale.US, "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#333\" stroke-width=\"2\"/>\n",
                marginLeft, marginTop + plotHeight, marginLeft + plotWidth, marginTop + plotHeight));

        svg.append("</svg>\n");
        return svg.toString();
    }
}
