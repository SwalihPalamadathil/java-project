package com.shop.model;

import java.time.LocalDateTime;

/**
 * POJO representing a Product in inventory.
 */
public class Product {
    private int id;
    private String name;
    private String sku; // Barcode / SKU
    private int categoryId;
    private String categoryName; // Helper field for UI display
    private String brand;
    private int supplierId;
    private String supplierName; // Helper field for UI display
    private String unit; // pcs, kg, litre, etc.
    private double purchasePrice;
    private double sellingPrice;
    private double taxRate; // in percent (e.g. 5.0, 18.0)
    private int currentStock;
    private int reorderLevel;
    private String expiryDate; // YYYY-MM-DD
    private String description;
    private boolean active;
    private LocalDateTime createdDate;

    public Product() {
        this.active = true;
        this.unit = "pcs";
        this.reorderLevel = 5;
    }

    public Product(int id, String name, String sku, int categoryId, String brand, int supplierId,
                   String unit, double purchasePrice, double sellingPrice, double taxRate,
                   int currentStock, int reorderLevel, String expiryDate, String description,
                   boolean active, LocalDateTime createdDate) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.categoryId = categoryId;
        this.brand = brand;
        this.supplierId = supplierId;
        this.unit = unit;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.taxRate = taxRate;
        this.currentStock = currentStock;
        this.reorderLevel = reorderLevel;
        this.expiryDate = expiryDate;
        this.description = description;
        this.active = active;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public boolean isLowStock() {
        return currentStock <= reorderLevel && currentStock > 0;
    }

    public boolean isOutOfStock() {
        return currentStock <= 0;
    }

    public String getStockStatus() {
        if (isOutOfStock()) return "OUT_OF_STOCK";
        if (isLowStock()) return "LOW_STOCK";
        return "IN_STOCK";
    }

    @Override
    public String toString() {
        return name + " (" + sku + ") - $" + String.format("%.2f", sellingPrice);
    }
}
