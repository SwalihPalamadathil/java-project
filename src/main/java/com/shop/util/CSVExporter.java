package com.shop.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 * Utility for exporting data collections to comma-separated values (CSV) format.
 */
public final class CSVExporter {

    private CSVExporter() {}

    /**
     * Exports tabular headers and records to a CSV file.
     *
     * @param headers List of column header names
     * @param rows List of data rows (each containing string values)
     * @param targetFile Destination CSV file
     * @throws Exception on I/O error
     */
    public static void exportToCsv(List<String> headers, List<List<String>> rows, File targetFile) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile))) {
            // Write Header row
            writer.println(formatLine(headers));

            // Write Data rows
            for (List<String> row : rows) {
                writer.println(formatLine(row));
            }
        }
    }

    private static String formatLine(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            String value = fields.get(i) != null ? fields.get(i) : "";
            // Escape quotes and wrap in quotes if containing comma, newline or quotes
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                value = "\"" + value.replace("\"", "\"\"") + "\"";
            }
            sb.append(value);
            if (i < fields.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
