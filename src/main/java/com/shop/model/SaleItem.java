package com.shop.model;

/**
 * POJO representing an individual line-item within a Sale invoice.
 */
public class SaleItem {
    private int id;
    private int saleId;
    private int productId;
    private String productName;
    private String sku;
    private double unitPrice;
    private int quantity;
    private double taxRate;
    private double taxAmount;
    private double discount;
    private double totalPrice;

    public SaleItem() {}

    public SaleItem(int id, int saleId, int productId, String productName, String sku,
                    double unitPrice, int quantity, double taxRate, double taxAmount,
                    double discount, double totalPrice) {
        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.discount = discount;
        this.totalPrice = totalPrice;
    }

    public SaleItem(int productId, String productName, String sku, double unitPrice,
                    int quantity, double taxRate, double discount) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.taxRate = taxRate;
        this.discount = discount;
        calculateTotals();
    }

    public void calculateTotals() {
        double base = unitPrice * quantity;
        double discounted = Math.max(0, base - discount);
        this.taxAmount = discounted * (taxRate / 100.0);
        this.totalPrice = discounted + taxAmount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        calculateTotals();
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        calculateTotals();
    }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
        calculateTotals();
    }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) {
        this.discount = discount;
        calculateTotals();
    }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}
