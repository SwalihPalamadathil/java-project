package com.shop.dao;

import com.shop.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for User entities.
 */
public interface UserDao {
    Optional<User> findById(int id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    boolean create(User user);
    boolean update(User user);
    boolean updatePassword(int userId, String newPasswordHash, boolean mustChangePassword);
    boolean incrementFailedAttempts(int userId);
    boolean resetFailedAttempts(int userId);
    boolean lockAccount(int userId);
    boolean unlockAccount(int userId);
}
