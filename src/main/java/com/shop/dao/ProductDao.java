package com.shop.dao;

import com.shop.model.Product;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Products inventory.
 */
public interface ProductDao {
    Optional<Product> findById(int id);
    Optional<Product> findBySku(String sku);
    List<Product> findAllActive();
    List<Product> searchAndFilter(String query, Integer categoryId, String stockStatus, int limit, int offset);
    int countSearchAndFilter(String query, Integer categoryId, String stockStatus);
    List<Product> findLowStockProducts();
    boolean create(Product product);
    boolean update(Product product);
    boolean softDelete(int id);
    boolean updateStock(int productId, int newStock);
    int getTotalActiveCount();
    int getLowStockCount();
    double getTotalInventoryValuation();
}
