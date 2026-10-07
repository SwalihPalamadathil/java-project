package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dao.ProductDaoImpl;
import com.shop.dao.StockMovementDao;
import com.shop.dao.StockMovementDaoImpl;
import com.shop.model.Product;
import com.shop.model.StockMovement;
import com.shop.util.DBConnection;
import com.shop.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Service managing Inventory stock movements, incoming purchases, adjustments, and valuations.
 */
public class StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockService.class);

    private final ProductDao productDao;
    private final StockMovementDao stockMovementDao;

    public StockService() {
        this(new ProductDaoImpl(), new StockMovementDaoImpl());
    }

    public StockService(ProductDao productDao, StockMovementDao stockMovementDao) {
        this.productDao = productDao;
        this.stockMovementDao = stockMovementDao;
    }

    /**
     * Records a stock-in (purchase from supplier) in a database transaction.
     */
    public boolean processStockIn(int productId, int quantity, double unitCost, String invoiceNumber, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock quantity must be greater than 0.");
        }

        Optional<Product> opt = productDao.findById(productId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with id: " + productId);
        }

        Product product = opt.get();
        int stockBefore = product.getCurrentStock();
        int stockAfter = stockBefore + quantity;

        String currentUser = SessionManager.getInstance().getCurrentUsername();

        try (Connection conn = DBConnection.createNewConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update product stock and optionally purchase price if updated
                productDao.updateStock(productId, stockAfter);

                StockMovement sm = new StockMovement();
                sm.setProductId(productId);
                sm.setType("PURCHASE_IN");
                sm.setQuantity(quantity);
                sm.setStockBefore(stockBefore);
                sm.setStockAfter(stockAfter);
                sm.setReason("Purchase from Supplier. " + (notes != null ? notes : ""));
                sm.setReferenceNumber(invoiceNumber);
                sm.setCreatedBy(currentUser);

                stockMovementDao.createWithConnection(sm, conn);

                conn.commit();
                logger.info("Processed Stock-In for product {} (Qty: {}, New Stock: {})", product.getName(), quantity, stockAfter);
                return true;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Failed to process Stock-In, rolled back: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to record stock purchase: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Database error during Stock-In: " + e.getMessage(), e);
        }
    }

    /**
     * Manually adjusts stock for damage, loss, or count discrepancy.
     * Strictly prevents negative inventory.
     */
    public boolean processStockAdjustment(int productId, String adjustmentType, int quantity, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Adjustment quantity must be greater than 0.");
        }

        Optional<Product> opt = productDao.findById(productId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Product not found.");
        }

        Product product = opt.get();
        int stockBefore = product.getCurrentStock();
        int stockAfter;

        if ("DAMAGE_LOSS".equalsIgnoreCase(adjustmentType) || "MANUAL_ADJUSTMENT_SUB".equalsIgnoreCase(adjustmentType)) {
            if (stockBefore < quantity) {
                throw new IllegalArgumentException("Insufficient stock! Cannot reduce " + quantity + " units (current stock is " + stockBefore + ").");
            }
            stockAfter = stockBefore - quantity;
        } else {
            // Addition
            stockAfter = stockBefore + quantity;
        }

        String currentUser = SessionManager.getInstance().getCurrentUsername();

        try (Connection conn = DBConnection.createNewConnection()) {
            conn.setAutoCommit(false);
            try {
                productDao.updateStock(productId, stockAfter);

                StockMovement sm = new StockMovement();
                sm.setProductId(productId);
                sm.setType(adjustmentType.contains("DAMAGE") ? "DAMAGE_LOSS" : "MANUAL_ADJUSTMENT");
                sm.setQuantity(quantity);
                sm.setStockBefore(stockBefore);
                sm.setStockAfter(stockAfter);
                sm.setReason(reason);
                sm.setReferenceNumber("ADJ-" + System.currentTimeMillis() % 100000);
                sm.setCreatedBy(currentUser);

                stockMovementDao.createWithConnection(sm, conn);

                conn.commit();
                logger.info("Stock adjusted for product {}: before={}, after={}", product.getName(), stockBefore, stockAfter);
                return true;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Stock adjustment failed: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to adjust stock: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Database error during stock adjustment: " + e.getMessage(), e);
        }
    }

    public List<StockMovement> getMovementHistory(int limit, int offset) {
        return stockMovementDao.findAll(limit, offset);
    }

    public List<StockMovement> getMovementHistoryForProduct(int productId, int limit) {
        return stockMovementDao.findByProductId(productId, limit);
    }

    public double getInventoryValuation() {
        return productDao.getTotalInventoryValuation();
    }
}
