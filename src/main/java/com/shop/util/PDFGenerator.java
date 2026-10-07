package com.shop.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import com.shop.model.SalaryRecord;
import com.shop.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Generates professional PDF documents for Invoices, Employee Payslips, and Reports.
 */
public final class PDFGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PDFGenerator.class);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font REGULAR_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    private PDFGenerator() {}

    /**
     * Generates a tax invoice receipt PDF for a completed sale.
     */
    public static void generateInvoicePdf(Sale sale, Map<String, String> shopSettings, File outputFile) throws Exception {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            // Shop Details Header
            String shopName = shopSettings.getOrDefault("shop_name", "Apex Supermarket");
            String shopAddress = shopSettings.getOrDefault("shop_address", "104 Market Road, City Centre");
            String shopPhone = shopSettings.getOrDefault("shop_phone", "+1 555-019-2834");
            String shopGst = shopSettings.getOrDefault("shop_gst", "29ABCDE1234F1Z5");
            String currency = shopSettings.getOrDefault("shop_currency", "$");

            Paragraph pShop = new Paragraph(shopName, TITLE_FONT);
            pShop.setAlignment(Element.ALIGN_CENTER);
            document.add(pShop);

            Paragraph pAddr = new Paragraph(shopAddress + " | Ph: " + shopPhone + " | GSTIN: " + shopGst, SUBTITLE_FONT);
            pAddr.setAlignment(Element.ALIGN_CENTER);
            pAddr.setSpacingAfter(15f);
            document.add(pAddr);

            // Invoice Info Bar
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

            String custInfo = "Customer: " + (sale.getCustomerName() != null && !sale.getCustomerName().isEmpty() ? sale.getCustomerName() : "Walk-in Customer")
                    + (sale.getCustomerPhone() != null && !sale.getCustomerPhone().isEmpty() ? "\nPhone: " + sale.getCustomerPhone() : "");
            PdfPCell cellLeft = new PdfPCell(new Phrase(custInfo, REGULAR_FONT));
            cellLeft.setBorder(PdfPCell.NO_BORDER);

            String invInfo = "Invoice #: " + sale.getInvoiceNumber()
                    + "\nDate: " + DateUtil.formatDisplayDateTime(sale.getCreatedAt() != null ? sale.getCreatedAt() : LocalDateTime.now())
                    + "\nBilled By: " + sale.getCreatedBy();
            PdfPCell cellRight = new PdfPCell(new Phrase(invInfo, REGULAR_FONT));
            cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellRight.setBorder(PdfPCell.NO_BORDER);

            metaTable.addCell(cellLeft);
            metaTable.addCell(cellRight);
            metaTable.setSpacingAfter(15f);
            document.add(metaTable);

            // Line Items Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 4f, 1.5f, 1.2f, 1.5f, 2f});

            addHeaderCell(table, "#");
            addHeaderCell(table, "Item Description");
            addHeaderCell(table, "Price");
            addHeaderCell(table, "Qty");
            addHeaderCell(table, "Tax");
            addHeaderCell(table, "Total");

            int idx = 1;
            for (SaleItem item : sale.getItems()) {
                addCell(table, String.valueOf(idx++), Element.ALIGN_CENTER);
                addCell(table, item.getProductName(), Element.ALIGN_LEFT);
                addCell(table, currency + String.format("%.2f", item.getUnitPrice()), Element.ALIGN_RIGHT);
                addCell(table, String.valueOf(item.getQuantity()), Element.ALIGN_CENTER);
                addCell(table, currency + String.format("%.2f", item.getTaxAmount()), Element.ALIGN_RIGHT);
                addCell(table, currency + String.format("%.2f", item.getTotalPrice()), Element.ALIGN_RIGHT);
            }
            table.setSpacingAfter(15f);
            document.add(table);

            // Summary Totals
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(45);
            summary.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addSummaryRow(summary, "Subtotal:", currency + String.format("%.2f", sale.getSubtotal()), false);
            addSummaryRow(summary, "Tax:", currency + String.format("%.2f", sale.getTaxAmount()), false);
            addSummaryRow(summary, "Discount:", "-" + currency + String.format("%.2f", sale.getDiscountAmount()), false);
            addSummaryRow(summary, "Grand Total:", currency + String.format("%.2f", sale.getGrandTotal()), true);
            addSummaryRow(summary, "Payment (" + sale.getPaymentMethod() + "):", currency + String.format("%.2f", sale.getAmountTendered()), false);
            addSummaryRow(summary, "Change Due:", currency + String.format("%.2f", sale.getChangeDue()), false);

            document.add(summary);

            // Footer
            Paragraph footer = new Paragraph("\nThank you for shopping with us! Please retain this invoice for any exchange within 7 days.", SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20f);
            document.add(footer);

        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        logger.info("Generated Invoice PDF at: {}", outputFile.getAbsolutePath());
    }

    /**
     * Generates an employee payslip PDF.
     */
    public static void generatePayslipPdf(SalaryRecord salary, Employee emp, Map<String, String> shopSettings, File outputFile) throws Exception {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            String shopName = shopSettings.getOrDefault("shop_name", "Apex Supermarket");
            String currency = shopSettings.getOrDefault("shop_currency", "$");

            Paragraph pShop = new Paragraph(shopName, TITLE_FONT);
            pShop.setAlignment(Element.ALIGN_CENTER);
            document.add(pShop);

            Paragraph pTitle = new Paragraph("PAYSLIP FOR " + salary.getMonthYear(), SUBTITLE_FONT);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            pTitle.setSpacingAfter(20f);
            document.add(pTitle);

            // Employee Details
            PdfPTable empTable = new PdfPTable(2);
            empTable.setWidthPercentage(100);
            empTable.setSpacingAfter(15f);

            empTable.addCell(new Phrase("Employee ID: EMP-" + emp.getId(), BOLD_FONT));
            empTable.addCell(new Phrase("Name: " + emp.getName(), BOLD_FONT));
            empTable.addCell(new Phrase("Designation: " + emp.getDesignation(), REGULAR_FONT));
            empTable.addCell(new Phrase("Status: " + salary.getPaymentStatus(), BOLD_FONT));
            document.add(empTable);

            // Earnings & Deductions Breakdown
            PdfPTable payTable = new PdfPTable(4);
            payTable.setWidthPercentage(100);
            payTable.setWidths(new float[]{3f, 2f, 3f, 2f});

            addHeaderCell(payTable, "Earnings Description");
            addHeaderCell(payTable, "Amount");
            addHeaderCell(payTable, "Deductions Description");
            addHeaderCell(payTable, "Amount");

            addCell(payTable, "Base Basic Salary", Element.ALIGN_LEFT);
            addCell(payTable, currency + String.format("%.2f", salary.getBaseSalary()), Element.ALIGN_RIGHT);
            addCell(payTable, "Absence / Leave Loss", Element.ALIGN_LEFT);
            addCell(payTable, currency + String.format("%.2f", salary.getAbsenceDeduction()), Element.ALIGN_RIGHT);

            addCell(payTable, "Overtime Allowance", Element.ALIGN_LEFT);
            addCell(payTable, currency + String.format("%.2f", salary.getOvertimePay()), Element.ALIGN_RIGHT);
            addCell(payTable, "Advance Salary Recovery", Element.ALIGN_LEFT);
            addCell(payTable, currency + String.format("%.2f", salary.getAdvanceDeduction()), Element.ALIGN_RIGHT);

            addCell(payTable, "Performance Bonus", Element.ALIGN_LEFT);
            addCell(payTable, currency + String.format("%.2f", salary.getBonus()), Element.ALIGN_RIGHT);
            addCell(payTable, "Other Deductions", Element.ALIGN_LEFT);
            addCell(payTable, currency + "0.00", Element.ALIGN_RIGHT);

            payTable.setSpacingAfter(15f);
            document.add(payTable);

            // Net Salary Highlight
            PdfPTable netTable = new PdfPTable(2);
            netTable.setWidthPercentage(50);
            netTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addSummaryRow(netTable, "NET SALARY PAYABLE:", currency + String.format("%.2f", salary.getNetSalary()), true);
            document.add(netTable);

            Paragraph sign = new Paragraph("\n\n\nEmployer Signature: _______________________      Employee Signature: _______________________", REGULAR_FONT);
            sign.setAlignment(Element.ALIGN_CENTER);
            document.add(sign);

        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        logger.info("Generated Payslip PDF at: {}", outputFile.getAbsolutePath());
    }

    /**
     * Generates a generic tabular report PDF (e.g. Sales, Profit, Inventory, Payroll).
     */
    public static void generateReportPdf(String title, String subtitle, List<String> headers,
                                         List<List<String>> rows, File outputFile) throws Exception {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            Paragraph pTitle = new Paragraph(title, TITLE_FONT);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitle);

            if (subtitle != null && !subtitle.isEmpty()) {
                Paragraph pSub = new Paragraph(subtitle, SUBTITLE_FONT);
                pSub.setAlignment(Element.ALIGN_CENTER);
                pSub.setSpacingAfter(15f);
                document.add(pSub);
            }

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            for (String h : headers) {
                addHeaderCell(table, h);
            }

            for (List<String> row : rows) {
                for (String cell : row) {
                    addCell(table, cell != null ? cell : "-", Element.ALIGN_LEFT);
                }
            }

            document.add(table);

            Paragraph footer = new Paragraph("\nGenerated on: " + DateUtil.nowString(), SMALL_FONT);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        logger.info("Generated Report PDF at: {}", outputFile.getAbsolutePath());
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(41, 128, 185)); // Elegant Blue
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private static void addCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, REGULAR_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private static void addSummaryRow(PdfPTable table, String label, String value, boolean isBold) {
        Font f = isBold ? BOLD_FONT : REGULAR_FONT;
        PdfPCell lCell = new PdfPCell(new Phrase(label, f));
        lCell.setBorder(PdfPCell.NO_BORDER);
        PdfPCell vCell = new PdfPCell(new Phrase(value, f));
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vCell.setBorder(PdfPCell.NO_BORDER);
        table.addCell(lCell);
        table.addCell(vCell);
    }
}
