package com.shop.model;

import java.time.LocalDateTime;

/**
 * POJO representing an audit log entry for inventory quantity changes.
 */
public class StockMovement {
    private int id;
    private int productId;
    private String productName; // Helper field for UI display
    private String type; // PURCHASE_IN, SALE_OUT, DAMAGE_LOSS, RETURN_IN, MANUAL_ADJUSTMENT
    private int quantity;
    private int stockBefore;
    private int stockAfter;
    private String reason;
    private String referenceNumber;
    private String createdBy;
    private LocalDateTime createdAt;

    public StockMovement() {}

    public StockMovement(int id, int productId, String productName, String type, int quantity,
                         int stockBefore, int stockAfter, String reason, String referenceNumber,
                         String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.reason = reason;
        this.referenceNumber = referenceNumber;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getStockBefore() { return stockBefore; }
    public void setStockBefore(int stockBefore) { this.stockBefore = stockBefore; }

    public int getStockAfter() { return stockAfter; }
    public void setStockAfter(int stockAfter) { this.stockAfter = stockAfter; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
