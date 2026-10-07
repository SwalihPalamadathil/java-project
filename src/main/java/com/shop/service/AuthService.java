package com.shop.service;

import com.shop.config.AppConfig;
import com.shop.dao.UserDao;
import com.shop.dao.UserDaoImpl;
import com.shop.model.User;
import com.shop.util.PasswordUtil;
import com.shop.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service managing user authentication, security lockouts, and session initialization.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao;

    public AuthService() {
        this(new UserDaoImpl());
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates a user with username and password.
     * Enforces account lockout after 5 consecutive failed attempts.
     *
     * @param username user identifier
     * @param plainPassword candidate plain password
     * @return User object on success
     * @throws SecurityException on authentication failure or account lockout
     */
    public User login(String username, String plainPassword) throws SecurityException {
        if (username == null || username.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            throw new SecurityException("Username and password are required.");
        }

        Optional<User> optUser = userDao.findByUsername(username.trim());
        if (optUser.isEmpty()) {
            logger.warn("Login attempt failed: user '{}' does not exist.", username);
            throw new SecurityException("Invalid username or password.");
        }

        User user = optUser.get();

        if (user.isLocked()) {
            logger.warn("Login rejected: account '{}' is locked.", username);
            throw new SecurityException("Account is locked due to multiple failed attempts. Please contact an Administrator.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            logger.warn("Login rejected: account '{}' is inactive.", username);
            throw new SecurityException("Account is inactive. Please contact an Administrator.");
        }

        boolean passwordMatch = PasswordUtil.checkPassword(plainPassword, user.getPasswordHash());
        if (!passwordMatch) {
            userDao.incrementFailedAttempts(user.getId());
            int newAttempts = user.getFailedAttempts() + 1;
            logger.warn("Failed login attempt #{} for user '{}'.", newAttempts, username);

            if (newAttempts >= AppConfig.MAX_FAILED_LOGIN_ATTEMPTS) {
                userDao.lockAccount(user.getId());
                logger.error("Account '{}' has been LOCKED after {} failed attempts.", username, newAttempts);
                throw new SecurityException("Account locked! You have exceeded " + AppConfig.MAX_FAILED_LOGIN_ATTEMPTS + " failed attempts.");
            }

            int remaining = AppConfig.MAX_FAILED_LOGIN_ATTEMPTS - newAttempts;
            throw new SecurityException("Invalid credentials. " + remaining + " attempt(s) remaining before lockout.");
        }

        // Reset failed attempts on successful login
        if (user.getFailedAttempts() > 0) {
            userDao.resetFailedAttempts(user.getId());
            user.setFailedAttempts(0);
        }

        // Establish session
        SessionManager.getInstance().login(user);
        logger.info("User '{}' ({}) successfully logged in.", user.getUsername(), user.getRole());
        return user;
    }

    /**
     * Changes user password and validates current password.
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        Optional<User> opt = userDao.findById(userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }

        User user = opt.get();
        if (!PasswordUtil.checkPassword(oldPassword, user.getPasswordHash())) {
            throw new SecurityException("Incorrect current password.");
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        boolean success = userDao.updatePassword(userId, newHash, false);
        if (success) {
            user.setPasswordHash(newHash);
            user.setMustChangePassword(false);
            logger.info("Password successfully updated for user '{}'.", user.getUsername());
        }
        return success;
    }

    /**
     * Creates a new staff or admin user.
     */
    public boolean registerUser(String username, String plainPassword, String fullName, String role) {
        if (userDao.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        String hash = PasswordUtil.hashPassword(plainPassword);
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setFullName(fullName);
        u.setRole(role);
        u.setStatus("ACTIVE");
        u.setFailedAttempts(0);
        u.setMustChangePassword(false);
        return userDao.create(u);
    }
}
