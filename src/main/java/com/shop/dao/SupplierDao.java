package com.shop.dao;

import com.shop.model.Supplier;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Product Suppliers.
 */
public interface SupplierDao {
    Optional<Supplier> findById(int id);
    List<Supplier> findAll();
    boolean create(Supplier supplier);
    boolean update(Supplier supplier);
    boolean delete(int id);
}
