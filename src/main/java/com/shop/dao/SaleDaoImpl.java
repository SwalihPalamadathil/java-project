package com.shop.dao;

import com.shop.model.Sale;
import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation of SaleDao using PreparedStatements.
 */
public class SaleDaoImpl implements SaleDao {

    private static final Logger logger = LoggerFactory.getLogger(SaleDaoImpl.class);

    @Override
    public boolean saveWithConnection(Sale sale, Connection conn) {
        String sql = "INSERT INTO sales (invoice_number, customer_name, customer_phone, subtotal, tax_amount, "
                + "discount_amount, grand_total, payment_method, amount_tendered, change_due, created_by, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sale.getInvoiceNumber());
            ps.setString(2, sale.getCustomerName());
            ps.setString(3, sale.getCustomerPhone());
            ps.setDouble(4, sale.getSubtotal());
            ps.setDouble(5, sale.getTaxAmount());
            ps.setDouble(6, sale.getDiscountAmount());
            ps.setDouble(7, sale.getGrandTotal());
            ps.setString(8, sale.getPaymentMethod());
            ps.setDouble(9, sale.getAmountTendered());
            ps.setDouble(10, sale.getChangeDue());
            ps.setString(11, sale.getCreatedBy());
            ps.setString(12, sale.getStatus() != null ? sale.getStatus() : "COMPLETED");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        sale.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error saving sale invoice: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Optional<Sale> findById(int id) {
        String sql = "SELECT * FROM sales WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSale(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding sale by id {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Sale> findByInvoiceNumber(String invoiceNumber) {
        String sql = "SELECT * FROM sales WHERE invoice_number = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSale(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding sale by invoice {}: {}", invoiceNumber, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Sale> search(String query, String startDate, String endDate, int limit, int offset) {
        List<Sale> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM sales WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        buildSearchClauses(sql, params, query, startDate, endDate);

        sql.append("ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 50);
        params.add(Math.max(0, offset));

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSale(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error in sale search: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public int countSearch(String query, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sales WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        buildSearchClauses(sql, params, query, startDate, endDate);

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting sales: {}", e.getMessage(), e);
        }
        return 0;
    }

    private void buildSearchClauses(StringBuilder sql, List<Object> params, String query, String startDate, String endDate) {
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(invoice_number) LIKE ? OR LOWER(customer_name) LIKE ? OR LOWER(customer_phone) LIKE ?) ");
            String term = "%" + query.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append("AND DATE(created_at) >= ? ");
            params.add(startDate.trim());
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append("AND DATE(created_at) <= ? ");
            params.add(endDate.trim());
        }
    }

    @Override
    public boolean updateStatusWithConnection(int saleId, String status, Connection conn) {
        String sql = "UPDATE sales SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, saleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating sale status: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public double getTodaySalesTotal() {
        String today = LocalDate.now().toString();
        String sql = "SELECT SUM(grand_total) FROM sales WHERE DATE(created_at) = ? AND status = 'COMPLETED'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting today's sales total: {}", e.getMessage(), e);
        }
        return 0.0;
    }

    @Override
    public double getMonthSalesTotal(String yearMonthPrefix) {
        String sql = "SELECT SUM(grand_total) FROM sales WHERE strftime('%Y-%m', created_at) = ? AND status = 'COMPLETED'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, yearMonthPrefix);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting month's sales total: {}", e.getMessage(), e);
        }
        return 0.0;
    }

    @Override
    public Map<String, Double> getSalesTrendsLast7Days() {
        Map<String, Double> map = new LinkedHashMap<>();
        // Initialize last 7 days with 0.0
        LocalDate now = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            map.put(now.minusDays(i).toString(), 0.0);
        }

        String sql = "SELECT DATE(created_at) AS sale_date, SUM(grand_total) AS total "
                + "FROM sales "
                + "WHERE DATE(created_at) >= DATE('now', '-6 days') AND status = 'COMPLETED' "
                + "GROUP BY DATE(created_at)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String d = rs.getString("sale_date");
                if (d != null && map.containsKey(d)) {
                    map.put(d, rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting last 7 days sales trends: {}", e.getMessage(), e);
        }
        return map;
    }

    @Override
    public Map<String, Double> getSalesTrendsLast12Months() {
        Map<String, Double> map = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            String ym = String.format("%04d-%02d", d.getYear(), d.getMonthValue());
            map.put(ym, 0.0);
        }

        String sql = "SELECT strftime('%Y-%m', created_at) AS ym, SUM(grand_total) AS total "
                + "FROM sales "
                + "WHERE status = 'COMPLETED' AND DATE(created_at) >= DATE('now', '-12 months') "
                + "GROUP BY strftime('%Y-%m', created_at)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String ym = rs.getString("ym");
                if (ym != null && map.containsKey(ym)) {
                    map.put(ym, rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting last 12 months sales trends: {}", e.getMessage(), e);
        }
        return map;
    }

    @Override
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT si.product_id, si.product_name, SUM(si.quantity) AS total_qty, SUM(si.total_price) AS total_revenue "
                + "FROM sale_items si "
                + "JOIN sales s ON si.sale_id = s.id "
                + "WHERE s.status = 'COMPLETED' "
                + "GROUP BY si.product_id, si.product_name "
                + "ORDER BY total_qty DESC LIMIT ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 5);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", rs.getInt("product_id"));
                    row.put("productName", rs.getString("product_name"));
                    row.put("totalQty", rs.getInt("total_qty"));
                    row.put("totalRevenue", rs.getDouble("total_revenue"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting top selling products: {}", e.getMessage(), e);
        }
        return list;
    }

    private Sale mapResultSetToSale(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNumber(rs.getString("invoice_number"));
        s.setCustomerName(rs.getString("customer_name"));
        s.setCustomerPhone(rs.getString("customer_phone"));
        s.setSubtotal(rs.getDouble("subtotal"));
        s.setTaxAmount(rs.getDouble("tax_amount"));
        s.setDiscountAmount(rs.getDouble("discount_amount"));
        s.setGrandTotal(rs.getDouble("grand_total"));
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setAmountTendered(rs.getDouble("amount_tendered"));
        s.setChangeDue(rs.getDouble("change_due"));
        s.setCreatedBy(rs.getString("created_by"));
        s.setStatus(rs.getString("status"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try {
                s.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
            } catch (Exception ignored) {}
        }
        return s;
    }
}
