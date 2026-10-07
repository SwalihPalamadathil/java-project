package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dao.ProductDaoImpl;
import com.shop.dao.SalaryRecordDao;
import com.shop.dao.SalaryRecordDaoImpl;
import com.shop.dao.SaleDao;
import com.shop.dao.SaleDaoImpl;
import com.shop.model.Product;
import com.shop.model.SalaryRecord;
import com.shop.model.Sale;
import com.shop.util.CSVExporter;
import com.shop.util.DBConnection;
import com.shop.util.PDFGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service managing analytics, sales and profit reporting, stock valuation, and PDF/CSV exports.
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final SaleDao saleDao;
    private final ProductDao productDao;
    private final SalaryRecordDao salaryRecordDao;

    public ReportService() {
        this(new SaleDaoImpl(), new ProductDaoImpl(), new SalaryRecordDaoImpl());
    }

    public ReportService(SaleDao saleDao, ProductDao productDao, SalaryRecordDao salaryRecordDao) {
        this.saleDao = saleDao;
        this.productDao = productDao;
        this.salaryRecordDao = salaryRecordDao;
    }

    /**
     * Generates a Sales Report for the specified date range.
     */
    public List<List<String>> getSalesReportData(String startDate, String endDate) {
        List<List<String>> rows = new ArrayList<>();
        List<Sale> sales = saleDao.search(null, startDate, endDate, 1000, 0);

        for (Sale s : sales) {
            rows.add(Arrays.asList(
                    s.getInvoiceNumber(),
                    s.getCreatedAt() != null ? s.getCreatedAt().toString().replace("T", " ") : "-",
                    s.getCustomerName() != null ? s.getCustomerName() : "Walk-in",
                    s.getPaymentMethod(),
                    String.format("%.2f", s.getSubtotal()),
                    String.format("%.2f", s.getTaxAmount()),
                    String.format("%.2f", s.getDiscountAmount()),
                    String.format("%.2f", s.getGrandTotal()),
                    s.getStatus()
            ));
        }
        return rows;
    }

    public List<String> getSalesHeaders() {
        return Arrays.asList("Invoice #", "Date & Time", "Customer", "Payment", "Subtotal", "Tax", "Discount", "Grand Total", "Status");
    }

    /**
     * Generates a Profit Report: Revenue - Cost of Goods Sold (COGS) = Gross Profit.
     */
    public List<List<String>> getProfitReportData(String startDate, String endDate) {
        List<List<String>> rows = new ArrayList<>();
        String sql = "SELECT p.name AS product_name, p.sku, "
                + "SUM(si.quantity) AS qty_sold, "
                + "p.purchase_price, "
                + "SUM(si.quantity * p.purchase_price) AS total_cogs, "
                + "SUM(si.total_price) AS total_revenue "
                + "FROM sale_items si "
                + "JOIN sales s ON si.sale_id = s.id "
                + "JOIN products p ON si.product_id = p.id "
                + "WHERE s.status = 'COMPLETED' "
                + (startDate != null && !startDate.isEmpty() ? "AND DATE(s.created_at) >= ? " : "")
                + (endDate != null && !endDate.isEmpty() ? "AND DATE(s.created_at) <= ? " : "")
                + "GROUP BY p.id, p.name, p.sku, p.purchase_price "
                + "ORDER BY total_revenue DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIdx = 1;
            if (startDate != null && !startDate.isEmpty()) {
                ps.setString(paramIdx++, startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                ps.setString(paramIdx++, endDate);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("product_name");
                    String sku = rs.getString("sku");
                    int qty = rs.getInt("qty_sold");
                    double unitCost = rs.getDouble("purchase_price");
                    double cogs = rs.getDouble("total_cogs");
                    double revenue = rs.getDouble("total_revenue");
                    double profit = revenue - cogs;
                    double margin = revenue > 0 ? (profit / revenue) * 100 : 0.0;

                    rows.add(Arrays.asList(
                            name + " (" + sku + ")",
                            String.valueOf(qty),
                            String.format("%.2f", unitCost),
                            String.format("%.2f", cogs),
                            String.format("%.2f", revenue),
                            String.format("%.2f", profit),
                            String.format("%.1f%%", margin)
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Error generating profit report: {}", e.getMessage(), e);
        }
        return rows;
    }

    public List<String> getProfitHeaders() {
        return Arrays.asList("Product", "Qty Sold", "Unit Cost", "Total Cost (COGS)", "Total Revenue", "Gross Profit", "Margin %");
    }

    /**
     * Generates a Stock and Valuation Report.
     */
    public List<List<String>> getStockReportData() {
        List<List<String>> rows = new ArrayList<>();
        List<Product> products = productDao.findAllActive();

        for (Product p : products) {
            double valuation = p.getCurrentStock() * p.getPurchasePrice();
            rows.add(Arrays.asList(
                    String.valueOf(p.getId()),
                    p.getName(),
                    p.getSku(),
                    p.getCategoryName() != null ? p.getCategoryName() : "-",
                    String.valueOf(p.getCurrentStock()),
                    String.valueOf(p.getReorderLevel()),
                    p.getStockStatus(),
                    String.format("%.2f", p.getPurchasePrice()),
                    String.format("%.2f", p.getSellingPrice()),
                    String.format("%.2f", valuation)
            ));
        }
        return rows;
    }

    public List<String> getStockHeaders() {
        return Arrays.asList("ID", "Product Name", "SKU", "Category", "Stock", "Reorder Level", "Status", "Purchase Price", "Selling Price", "Stock Value");
    }

    /**
     * Generates an Employee Payroll Summary Report.
     */
    public List<List<String>> getPayrollReportData(String monthYear) {
        List<List<String>> rows = new ArrayList<>();
        List<SalaryRecord> records = (monthYear != null && !monthYear.isEmpty())
                ? salaryRecordDao.findByMonth(monthYear)
                : salaryRecordDao.findAll();

        for (SalaryRecord sr : records) {
            rows.add(Arrays.asList(
                    sr.getMonthYear(),
                    sr.getEmployeeName(),
                    String.format("%.2f", sr.getBaseSalary()),
                    String.format("%.2f", sr.getAbsenceDeduction()),
                    String.format("%.2f", sr.getOvertimePay()),
                    String.format("%.2f", sr.getBonus()),
                    String.format("%.2f", sr.getAdvanceDeduction()),
                    String.format("%.2f", sr.getNetSalary()),
                    sr.getPaymentStatus(),
                    sr.getPaymentDate() != null ? sr.getPaymentDate() : "-"
            ));
        }
        return rows;
    }

    public List<String> getPayrollHeaders() {
        return Arrays.asList("Month", "Employee Name", "Base Salary", "Absence Ded.", "Overtime", "Bonus", "Advance Ded.", "Net Salary", "Status", "Paid Date");
    }

    public void exportReportToPdf(String title, String subtitle, List<String> headers, List<List<String>> rows, File file) throws Exception {
        PDFGenerator.generateReportPdf(title, subtitle, headers, rows, file);
    }

    public void exportReportToCsv(List<String> headers, List<List<String>> rows, File file) throws Exception {
        CSVExporter.exportToCsv(headers, rows, file);
    }
}
