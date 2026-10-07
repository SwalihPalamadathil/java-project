package com.shop.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * POJO representing a Sale (Invoice) transaction.
 */
public class Sale {
    private int id;
    private String invoiceNumber;
    private String customerName;
    private String customerPhone;
    private double subtotal;
    private double taxAmount;
    private double discountAmount;
    private double grandTotal;
    private String paymentMethod; // CASH, CARD, UPI
    private double amountTendered;
    private double changeDue;
    private String createdBy;
    private LocalDateTime createdAt;
    private String status; // COMPLETED, REFUNDED

    private List<SaleItem> items = new ArrayList<>();

    public Sale() {
        this.status = "COMPLETED";
        this.paymentMethod = "CASH";
    }

    public Sale(int id, String invoiceNumber, String customerName, String customerPhone,
                double subtotal, double taxAmount, double discountAmount, double grandTotal,
                String paymentMethod, double amountTendered, double changeDue, String createdBy,
                LocalDateTime createdAt, String status) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.discountAmount = discountAmount;
        this.grandTotal = grandTotal;
        this.paymentMethod = paymentMethod;
        this.amountTendered = amountTendered;
        this.changeDue = changeDue;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getAmountTendered() { return amountTendered; }
    public void setAmountTendered(double amountTendered) { this.amountTendered = amountTendered; }

    public double getChangeDue() { return changeDue; }
    public void setChangeDue(double changeDue) { this.changeDue = changeDue; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
}
