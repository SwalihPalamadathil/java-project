package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dao.ProductDaoImpl;
import com.shop.dao.SaleDao;
import com.shop.dao.SaleDaoImpl;
import com.shop.dao.SaleItemDao;
import com.shop.dao.SaleItemDaoImpl;
import com.shop.dao.SettingDao;
import com.shop.dao.SettingDaoImpl;
import com.shop.dao.StockMovementDao;
import com.shop.dao.StockMovementDaoImpl;
import com.shop.model.Product;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import com.shop.model.StockMovement;
import com.shop.util.DBConnection;
import com.shop.util.DateUtil;
import com.shop.util.PDFGenerator;
import com.shop.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing POS checkout transactions, invoice generation, sales history, and returns.
 */
public class SalesService {

    private static final Logger logger = LoggerFactory.getLogger(SalesService.class);

    private final SaleDao saleDao;
    private final SaleItemDao saleItemDao;
    private final ProductDao productDao;
    private final StockMovementDao stockMovementDao;
    private final SettingDao settingDao;

    public SalesService() {
        this(new SaleDaoImpl(), new SaleItemDaoImpl(), new ProductDaoImpl(), new StockMovementDaoImpl(), new SettingDaoImpl());
    }

    public SalesService(SaleDao saleDao, SaleItemDao saleItemDao, ProductDao productDao,
                        StockMovementDao stockMovementDao, SettingDao settingDao) {
        this.saleDao = saleDao;
        this.saleItemDao = saleItemDao;
        this.productDao = productDao;
        this.stockMovementDao = stockMovementDao;
        this.settingDao = settingDao;
    }

    /**
     * Executes a complete Sale transaction atomically.
     * Enforces stock validation, writes invoice + items, decrements inventory, and logs audit movements.
     */
    public Sale processCheckout(Sale sale) throws Exception {
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty.");
        }

        if (sale.getGrandTotal() < 0) {
            throw new IllegalArgumentException("Grand total cannot be negative.");
        }

        if (sale.getAmountTendered() < sale.getGrandTotal()) {
            throw new IllegalArgumentException("Amount tendered ($" + String.format("%.2f", sale.getAmountTendered())
                    + ") is less than the grand total ($" + String.format("%.2f", sale.getGrandTotal()) + ").");
        }

        // Calculate change due
        sale.setChangeDue(Math.max(0.0, sale.getAmountTendered() - sale.getGrandTotal()));

        // Generate invoice number if not already generated
        if (sale.getInvoiceNumber() == null || sale.getInvoiceNumber().isEmpty()) {
            sale.setInvoiceNumber(generateUniqueInvoiceNumber());
        }

        sale.setCreatedBy(SessionManager.getInstance().getCurrentUsername());

        try (Connection conn = DBConnection.createNewConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Validate stock availability and lock/fetch current stock
                for (SaleItem item : sale.getItems()) {
                    Optional<Product> opt = productDao.findById(item.getProductId());
                    if (opt.isEmpty()) {
                        throw new IllegalStateException("Product '" + item.getProductName() + "' no longer exists.");
                    }
                    Product p = opt.get();
                    if (p.getCurrentStock() < item.getQuantity()) {
                        throw new IllegalStateException("Insufficient stock for '" + p.getName()
                                + "'. Available: " + p.getCurrentStock() + ", Requested: " + item.getQuantity());
                    }
                }

                // 2. Save Sale Master record
                boolean savedSale = saleDao.saveWithConnection(sale, conn);
                if (!savedSale || sale.getId() == 0) {
                    throw new RuntimeException("Failed to persist sales record.");
                }

                // 3. Save Line Items, Decrement Stock, and Write Stock Movement
                for (SaleItem item : sale.getItems()) {
                    item.setSaleId(sale.getId());
                    saleItemDao.saveWithConnection(item, conn);

                    // Fetch latest stock
                    Product p = productDao.findById(item.getProductId()).orElseThrow();
                    int stockBefore = p.getCurrentStock();
                    int stockAfter = stockBefore - item.getQuantity();

                    // Update product stock directly in this transaction
                    String updateStockSql = "UPDATE products SET current_stock = ? WHERE id = ?";
                    try (PreparedStatement psStock = conn.prepareStatement(updateStockSql)) {
                        psStock.setInt(1, stockAfter);
                        psStock.setInt(2, item.getProductId());
                        psStock.executeUpdate();
                    }

                    // Audit Movement
                    StockMovement sm = new StockMovement();
                    sm.setProductId(item.getProductId());
                    sm.setType("SALE_OUT");
                    sm.setQuantity(item.getQuantity());
                    sm.setStockBefore(stockBefore);
                    sm.setStockAfter(stockAfter);
                    sm.setReason("Sale Invoice #" + sale.getInvoiceNumber());
                    sm.setReferenceNumber(sale.getInvoiceNumber());
                    sm.setCreatedBy(sale.getCreatedBy());

                    stockMovementDao.createWithConnection(sm, conn);
                }

                conn.commit();
                logger.info("Checkout completed successfully. Invoice #{}", sale.getInvoiceNumber());
                return sale;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Checkout transaction failed, rolled back: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Processes a sale return / refund transaction. Restores product inventory.
     */
    public boolean processReturnAndRefund(int saleId, String reason) throws Exception {
        Optional<Sale> optSale = saleDao.findById(saleId);
        if (optSale.isEmpty()) {
            throw new IllegalArgumentException("Sale not found.");
        }

        Sale sale = optSale.get();
        if ("REFUNDED".equalsIgnoreCase(sale.getStatus())) {
            throw new IllegalStateException("This sale has already been refunded.");
        }

        List<SaleItem> items = saleItemDao.findBySaleId(saleId);
        String currentUser = SessionManager.getInstance().getCurrentUsername();

        try (Connection conn = DBConnection.createNewConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Update sale status
                saleDao.updateStatusWithConnection(saleId, "REFUNDED", conn);

                // 2. Restock each item
                for (SaleItem item : items) {
                    Product p = productDao.findById(item.getProductId()).orElseThrow();
                    int stockBefore = p.getCurrentStock();
                    int stockAfter = stockBefore + item.getQuantity();

                    String updateStockSql = "UPDATE products SET current_stock = ? WHERE id = ?";
                    try (PreparedStatement psStock = conn.prepareStatement(updateStockSql)) {
                        psStock.setInt(1, stockAfter);
                        psStock.setInt(2, item.getProductId());
                        psStock.executeUpdate();
                    }

                    // Audit Movement
                    StockMovement sm = new StockMovement();
                    sm.setProductId(item.getProductId());
                    sm.setType("RETURN_IN");
                    sm.setQuantity(item.getQuantity());
                    sm.setStockBefore(stockBefore);
                    sm.setStockAfter(stockAfter);
                    sm.setReason("Refund for Invoice #" + sale.getInvoiceNumber() + ". " + (reason != null ? reason : ""));
                    sm.setReferenceNumber(sale.getInvoiceNumber());
                    sm.setCreatedBy(currentUser);

                    stockMovementDao.createWithConnection(sm, conn);
                }

                conn.commit();
                logger.info("Refund processed for Invoice #{}", sale.getInvoiceNumber());
                return true;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Refund failed, rolled back: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Generates a unique sequential invoice number in the format INV-YYYYMMDD-XXXX.
     */
    public String generateUniqueInvoiceNumber() {
        String datePrefix = LocalDate.now().format(DateUtil.INVOICE_DATE_FORMATTER);
        long timestampSuffix = System.currentTimeMillis() % 10000;
        return String.format("INV-%s-%04d", datePrefix, timestampSuffix);
    }

    public List<Sale> searchSales(String query, String startDate, String endDate, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return saleDao.search(query, startDate, endDate, pageSize, offset);
    }

    public int countSales(String query, String startDate, String endDate) {
        return saleDao.countSearch(query, startDate, endDate);
    }

    public List<SaleItem> getSaleItems(int saleId) {
        return saleItemDao.findBySaleId(saleId);
    }

    public void exportInvoicePdf(Sale sale, File targetFile) throws Exception {
        Map<String, String> settings = settingDao.getAll();
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            sale.setItems(saleItemDao.findBySaleId(sale.getId()));
        }
        PDFGenerator.generateInvoicePdf(sale, settings, targetFile);
    }
}
