package com.shop.service;

import com.shop.dao.CategoryDao;
import com.shop.dao.CategoryDaoImpl;
import com.shop.dao.ProductDao;
import com.shop.dao.ProductDaoImpl;
import com.shop.dao.SupplierDao;
import com.shop.dao.SupplierDaoImpl;
import com.shop.model.Category;
import com.shop.model.Product;
import com.shop.model.Supplier;
import com.shop.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service managing business logic for Products, Categories, and Suppliers.
 */
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final SupplierDao supplierDao;

    public ProductService() {
        this(new ProductDaoImpl(), new CategoryDaoImpl(), new SupplierDaoImpl());
    }

    public ProductService(ProductDao productDao, CategoryDao categoryDao, SupplierDao supplierDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.supplierDao = supplierDao;
    }

    // ==========================================
    // PRODUCT OPERATIONS
    // ==========================================

    public Optional<Product> getProductById(int id) {
        return productDao.findById(id);
    }

    public Optional<Product> getProductBySku(String sku) {
        if (!Validator.isNotEmpty(sku)) return Optional.empty();
        return productDao.findBySku(sku.trim());
    }

    public List<Product> getAllActiveProducts() {
        return productDao.findAllActive();
    }

    public List<Product> searchProducts(String query, Integer categoryId, String stockStatus, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return productDao.searchAndFilter(query, categoryId, stockStatus, pageSize, offset);
    }

    public int countProducts(String query, Integer categoryId, String stockStatus) {
        return productDao.countSearchAndFilter(query, categoryId, stockStatus);
    }

    public List<Product> getLowStockProducts() {
        return productDao.findLowStockProducts();
    }

    public boolean saveProduct(Product product) {
        validateProduct(product);

        // Check SKU uniqueness
        Optional<Product> existing = productDao.findBySku(product.getSku());
        if (product.getId() == 0) {
            // New product
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Product SKU/Barcode '" + product.getSku() + "' already exists!");
            }
            return productDao.create(product);
        } else {
            // Updating existing product
            if (existing.isPresent() && existing.get().getId() != product.getId()) {
                throw new IllegalArgumentException("Product SKU/Barcode '" + product.getSku() + "' is already used by another product!");
            }
            return productDao.update(product);
        }
    }

    public boolean deleteProduct(int productId) {
        return productDao.softDelete(productId);
    }

    private void validateProduct(Product p) {
        if (!Validator.isNotEmpty(p.getName())) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (!Validator.isNotEmpty(p.getSku())) {
            throw new IllegalArgumentException("Product SKU / Barcode is required.");
        }
        if (!Validator.isNonNegative(p.getPurchasePrice())) {
            throw new IllegalArgumentException("Purchase price must be greater than or equal to 0.");
        }
        if (!Validator.isNonNegative(p.getSellingPrice())) {
            throw new IllegalArgumentException("Selling price must be greater than or equal to 0.");
        }
        if (!Validator.isNonNegative(p.getTaxRate())) {
            throw new IllegalArgumentException("Tax rate must be non-negative.");
        }
        if (!Validator.isNonNegative(p.getReorderLevel())) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }
    }

    // ==========================================
    // CATEGORY OPERATIONS
    // ==========================================

    public List<Category> getAllCategories() {
        return categoryDao.findAll();
    }

    public boolean saveCategory(Category category) {
        if (!Validator.isNotEmpty(category.getName())) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        Optional<Category> existing = categoryDao.findByName(category.getName());
        if (category.getId() == 0) {
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists.");
            }
            return categoryDao.create(category);
        } else {
            if (existing.isPresent() && existing.get().getId() != category.getId()) {
                throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists.");
            }
            return categoryDao.update(category);
        }
    }

    public boolean deleteCategory(int id) {
        return categoryDao.delete(id);
    }

    // ==========================================
    // SUPPLIER OPERATIONS
    // ==========================================

    public List<Supplier> getAllSuppliers() {
        return supplierDao.findAll();
    }

    public boolean saveSupplier(Supplier supplier) {
        if (!Validator.isNotEmpty(supplier.getName())) {
            throw new IllegalArgumentException("Supplier company name is required.");
        }
        if (supplier.getId() == 0) {
            return supplierDao.create(supplier);
        } else {
            return supplierDao.update(supplier);
        }
    }

    public boolean deleteSupplier(int id) {
        return supplierDao.delete(id);
    }
}
