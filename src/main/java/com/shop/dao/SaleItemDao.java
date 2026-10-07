package com.shop.dao;

import com.shop.model.SaleItem;
import java.sql.Connection;
import java.util.List;

/**
 * Data Access Object interface for Sale Line Items.
 */
public interface SaleItemDao {
    boolean saveWithConnection(SaleItem item, Connection conn);
    List<SaleItem> findBySaleId(int saleId);
}
