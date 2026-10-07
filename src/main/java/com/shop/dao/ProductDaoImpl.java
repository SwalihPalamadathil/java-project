package com.shop.dao;

import com.shop.model.Product;
import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ProductDao using PreparedStatements.
 */
public class ProductDaoImpl implements ProductDao {

    private static final Logger logger = LoggerFactory.getLogger(ProductDaoImpl.class);

    private static final String BASE_SELECT =
            "SELECT p.*, c.name AS category_name, s.name AS supplier_name "
            + "FROM products p "
            + "LEFT JOIN categories c ON p.category_id = c.id "
            + "LEFT JOIN suppliers s ON p.supplier_id = s.id ";

    @Override
    public Optional<Product> findById(int id) {
        String sql = BASE_SELECT + "WHERE p.id = ? AND p.is_active = 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding product by id {}: {}", id, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        String sql = BASE_SELECT + "WHERE LOWER(p.sku) = LOWER(?) AND p.is_active = 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku != null ? sku.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding product by SKU {}: {}", sku, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Product> findAllActive() {
        List<Product> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE p.is_active = 1 ORDER BY p.name ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all active products: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Product> searchAndFilter(String query, Integer categoryId, String stockStatus, int limit, int offset) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE p.is_active = 1 ");
        List<Object> params = new ArrayList<>();

        buildFilterClauses(sql, params, query, categoryId, stockStatus);

        sql.append("ORDER BY p.name ASC LIMIT ? OFFSET ?");
        params.add(limit > 0 ? limit : 50);
        params.add(Math.max(0, offset));

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error in product searchAndFilter: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public int countSearchAndFilter(String query, Integer categoryId, String stockStatus) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM products p WHERE p.is_active = 1 ");
        List<Object> params = new ArrayList<>();

        buildFilterClauses(sql, params, query, categoryId, stockStatus);

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
            logger.error("Error counting searchAndFilter products: {}", e.getMessage(), e);
        }
        return 0;
    }

    private void buildFilterClauses(StringBuilder sql, List<Object> params, String query, Integer categoryId, String stockStatus) {
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(p.name) LIKE ? OR LOWER(p.sku) LIKE ? OR LOWER(p.brand) LIKE ?) ");
            String term = "%" + query.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }
        if (categoryId != null && categoryId > 0) {
            sql.append("AND p.category_id = ? ");
            params.add(categoryId);
        }
        if ("LOW_STOCK".equalsIgnoreCase(stockStatus)) {
            sql.append("AND p.current_stock <= p.reorder_level AND p.current_stock > 0 ");
        } else if ("OUT_OF_STOCK".equalsIgnoreCase(stockStatus)) {
            sql.append("AND p.current_stock <= 0 ");
        } else if ("IN_STOCK".equalsIgnoreCase(stockStatus)) {
            sql.append("AND p.current_stock > p.reorder_level ");
        }
    }

    @Override
    public List<Product> findLowStockProducts() {
        List<Product> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE p.is_active = 1 AND p.current_stock <= p.reorder_level ORDER BY p.current_stock ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching low stock products: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean create(Product product) {
        String sql = "INSERT INTO products (name, sku, category_id, brand, supplier_id, unit, purchase_price, "
                + "selling_price, tax_rate, current_stock, reorder_level, expiry_date, description, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getSku());
            ps.setObject(3, product.getCategoryId() > 0 ? product.getCategoryId() : null);
            ps.setString(4, product.getBrand());
            ps.setObject(5, product.getSupplierId() > 0 ? product.getSupplierId() : null);
            ps.setString(6, product.getUnit() != null ? product.getUnit() : "pcs");
            ps.setDouble(7, product.getPurchasePrice());
            ps.setDouble(8, product.getSellingPrice());
            ps.setDouble(9, product.getTaxRate());
            ps.setInt(10, product.getCurrentStock());
            ps.setInt(11, product.getReorderLevel());
            ps.setString(12, product.getExpiryDate());
            ps.setString(13, product.getDescription());
            ps.setInt(14, product.isActive() ? 1 : 0);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        product.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error creating product {}: {}", product.getName(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean update(Product product) {
        String sql = "UPDATE products SET name = ?, sku = ?, category_id = ?, brand = ?, supplier_id = ?, unit = ?, "
                + "purchase_price = ?, selling_price = ?, tax_rate = ?, reorder_level = ?, expiry_date = ?, description = ? "
                + "WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getSku());
            ps.setObject(3, product.getCategoryId() > 0 ? product.getCategoryId() : null);
            ps.setString(4, product.getBrand());
            ps.setObject(5, product.getSupplierId() > 0 ? product.getSupplierId() : null);
            ps.setString(6, product.getUnit());
            ps.setDouble(7, product.getPurchasePrice());
            ps.setDouble(8, product.getSellingPrice());
            ps.setDouble(9, product.getTaxRate());
            ps.setInt(10, product.getReorderLevel());
            ps.setString(11, product.getExpiryDate());
            ps.setString(12, product.getDescription());
            ps.setInt(13, product.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating product {}: {}", product.getId(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean softDelete(int id) {
        String sql = "UPDATE products SET is_active = 0 WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error soft deleting product {}: {}", id, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateStock(int productId, int newStock) {
        String sql = "UPDATE products SET current_stock = ? WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating stock for product {}: {}", productId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public int getTotalActiveCount() {
        String sql = "SELECT COUNT(*) FROM products WHERE is_active = 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting total active product count: {}", e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public int getLowStockCount() {
        String sql = "SELECT COUNT(*) FROM products WHERE is_active = 1 AND current_stock <= reorder_level";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting low stock count: {}", e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public double getTotalInventoryValuation() {
        String sql = "SELECT SUM(current_stock * purchase_price) FROM products WHERE is_active = 1";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            logger.error("Error calculating total inventory valuation: {}", e.getMessage(), e);
        }
        return 0.0;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setSku(rs.getString("sku"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setBrand(rs.getString("brand"));
        p.setSupplierId(rs.getInt("supplier_id"));
        p.setSupplierName(rs.getString("supplier_name"));
        p.setUnit(rs.getString("unit"));
        p.setPurchasePrice(rs.getDouble("purchase_price"));
        p.setSellingPrice(rs.getDouble("selling_price"));
        p.setTaxRate(rs.getDouble("tax_rate"));
        p.setCurrentStock(rs.getInt("current_stock"));
        p.setReorderLevel(rs.getInt("reorder_level"));
        p.setExpiryDate(rs.getString("expiry_date"));
        p.setDescription(rs.getString("description"));
        p.setActive(rs.getInt("is_active") == 1);
        String created = rs.getString("created_date");
        if (created != null) {
            try {
                p.setCreatedDate(LocalDateTime.parse(created.replace(" ", "T")));
            } catch (Exception ignored) {}
        }
        return p;
    }
}
