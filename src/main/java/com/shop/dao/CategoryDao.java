package com.shop.dao;

import com.shop.model.Category;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Product Categories.
 */
public interface CategoryDao {
    Optional<Category> findById(int id);
    Optional<Category> findByName(String name);
    List<Category> findAll();
    boolean create(Category category);
    boolean update(Category category);
    boolean delete(int id);
}
