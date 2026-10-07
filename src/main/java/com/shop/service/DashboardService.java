package com.shop.service;

import com.shop.dao.EmployeeDao;
import com.shop.dao.EmployeeDaoImpl;
import com.shop.dao.ProductDao;
import com.shop.dao.ProductDaoImpl;
import com.shop.dao.SaleDao;
import com.shop.dao.SaleDaoImpl;
import com.shop.model.Product;
import com.shop.util.DateUtil;

import java.util.List;
import java.util.Map;

/**
 * Service providing aggregated metrics, KPI cards, and trend charts for the main Dashboard.
 */
public class DashboardService {

    private final SaleDao saleDao;
    private final ProductDao productDao;
    private final EmployeeDao employeeDao;

    public DashboardService() {
        this(new SaleDaoImpl(), new ProductDaoImpl(), new EmployeeDaoImpl());
    }

    public DashboardService(SaleDao saleDao, ProductDao productDao, EmployeeDao employeeDao) {
        this.saleDao = saleDao;
        this.productDao = productDao;
        this.employeeDao = employeeDao;
    }

    public double getTodaySalesTotal() {
        return saleDao.getTodaySalesTotal();
    }

    public double getCurrentMonthRevenue() {
        return saleDao.getMonthSalesTotal(DateUtil.currentMonthYear());
    }

    public int getTotalProductCount() {
        return productDao.getTotalActiveCount();
    }

    public int getLowStockProductCount() {
        return productDao.getLowStockCount();
    }

    public int getTotalEmployeeCount() {
        return employeeDao.getTotalActiveCount();
    }

    public Map<String, Double> getLast7DaysSales() {
        return saleDao.getSalesTrendsLast7Days();
    }

    public Map<String, Double> getLast12MonthsSales() {
        return saleDao.getSalesTrendsLast12Months();
    }

    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        return saleDao.getTopSellingProducts(limit);
    }

    public List<Product> getLowStockAlertList() {
        return productDao.findLowStockProducts();
    }
}
