package com.shop.dao;

import com.shop.model.StockMovement;
import java.sql.Connection;
import java.util.List;

/**
 * Data Access Object interface for Stock Movement audit logging.
 */
public interface StockMovementDao {
    boolean create(StockMovement movement);
    boolean createWithConnection(StockMovement movement, Connection conn);
    List<StockMovement> findAll(int limit, int offset);
    List<StockMovement> findByProductId(int productId, int limit);
}
