package com.shop.dao;

import com.shop.model.Sale;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object interface for Sales invoices.
 */
public interface SaleDao {
    boolean saveWithConnection(Sale sale, Connection conn);
    Optional<Sale> findById(int id);
    Optional<Sale> findByInvoiceNumber(String invoiceNumber);
    List<Sale> search(String query, String startDate, String endDate, int limit, int offset);
    int countSearch(String query, String startDate, String endDate);
    boolean updateStatusWithConnection(int saleId, String status, Connection conn);

    double getTodaySalesTotal();
    double getMonthSalesTotal(String yearMonthPrefix); // YYYY-MM
    Map<String, Double> getSalesTrendsLast7Days();
    Map<String, Double> getSalesTrendsLast12Months();
    List<Map<String, Object>> getTopSellingProducts(int limit);
}
