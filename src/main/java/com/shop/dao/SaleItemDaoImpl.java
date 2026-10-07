package com.shop.dao;

import com.shop.model.SaleItem;
import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of SaleItemDao.
 */
public class SaleItemDaoImpl implements SaleItemDao {

    private static final Logger logger = LoggerFactory.getLogger(SaleItemDaoImpl.class);

    @Override
    public boolean saveWithConnection(SaleItem item, Connection conn) {
        String sql = "INSERT INTO sale_items (sale_id, product_id, product_name, sku, unit_price, quantity, "
                + "tax_rate, tax_amount, discount, total_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getSaleId());
            ps.setInt(2, item.getProductId());
            ps.setString(3, item.getProductName());
            ps.setString(4, item.getSku());
            ps.setDouble(5, item.getUnitPrice());
            ps.setInt(6, item.getQuantity());
            ps.setDouble(7, item.getTaxRate());
            ps.setDouble(8, item.getTaxAmount());
            ps.setDouble(9, item.getDiscount());
            ps.setDouble(10, item.getTotalPrice());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        item.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error saving sale item: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<SaleItem> findBySaleId(int saleId) {
        List<SaleItem> list = new ArrayList<>();
        String sql = "SELECT * FROM sale_items WHERE sale_id = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem();
                    item.setId(rs.getInt("id"));
                    item.setSaleId(rs.getInt("sale_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setSku(rs.getString("sku"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setTaxRate(rs.getDouble("tax_rate"));
                    item.setTaxAmount(rs.getDouble("tax_amount"));
                    item.setDiscount(rs.getDouble("discount"));
                    item.setTotalPrice(rs.getDouble("total_price"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching items for sale id {}: {}", saleId, e.getMessage(), e);
        }
        return list;
    }
}
