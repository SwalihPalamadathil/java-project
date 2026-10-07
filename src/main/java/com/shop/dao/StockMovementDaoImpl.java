package com.shop.dao;

import com.shop.model.StockMovement;
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

/**
 * JDBC implementation of StockMovementDao.
 */
public class StockMovementDaoImpl implements StockMovementDao {

    private static final Logger logger = LoggerFactory.getLogger(StockMovementDaoImpl.class);

    private static final String BASE_SELECT =
            "SELECT sm.*, p.name AS product_name "
            + "FROM stock_movements sm "
            + "JOIN products p ON sm.product_id = p.id ";

    @Override
    public boolean create(StockMovement movement) {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            return createWithConnection(movement, conn);
        } catch (SQLException e) {
            logger.error("Error creating stock movement: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean createWithConnection(StockMovement movement, Connection conn) {
        String sql = "INSERT INTO stock_movements (product_id, type, quantity, stock_before, stock_after, "
                + "reason, reference_number, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, movement.getProductId());
            ps.setString(2, movement.getType());
            ps.setInt(3, movement.getQuantity());
            ps.setInt(4, movement.getStockBefore());
            ps.setInt(5, movement.getStockAfter());
            ps.setString(6, movement.getReason());
            ps.setString(7, movement.getReferenceNumber());
            ps.setString(8, movement.getCreatedBy());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        movement.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error executing createWithConnection for stock movement: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<StockMovement> findAll(int limit, int offset) {
        List<StockMovement> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY sm.created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 100);
            ps.setInt(2, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToStockMovement(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching all stock movements: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<StockMovement> findByProductId(int productId, int limit) {
        List<StockMovement> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE sm.product_id = ? ORDER BY sm.created_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToStockMovement(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching stock movements for product {}: {}", productId, e.getMessage(), e);
        }
        return list;
    }

    private StockMovement mapResultSetToStockMovement(ResultSet rs) throws SQLException {
        StockMovement sm = new StockMovement();
        sm.setId(rs.getInt("id"));
        sm.setProductId(rs.getInt("product_id"));
        sm.setProductName(rs.getString("product_name"));
        sm.setType(rs.getString("type"));
        sm.setQuantity(rs.getInt("quantity"));
        sm.setStockBefore(rs.getInt("stock_before"));
        sm.setStockAfter(rs.getInt("stock_after"));
        sm.setReason(rs.getString("reason"));
        sm.setReferenceNumber(rs.getString("reference_number"));
        sm.setCreatedBy(rs.getString("created_by"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try {
                sm.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
            } catch (Exception ignored) {}
        }
        return sm;
    }
}
