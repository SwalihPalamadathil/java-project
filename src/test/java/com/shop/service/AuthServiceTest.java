package com.shop.service;

import com.shop.dao.UserDao;
import com.shop.model.User;
import com.shop.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private UserDao mockUserDao;
    private AuthService authService;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testadmin");
        testUser.setPasswordHash(PasswordUtil.hashPassword("Password@123"));
        testUser.setFullName("Test Admin");
        testUser.setRole("ADMIN");
        testUser.setStatus("ACTIVE");
        testUser.setFailedAttempts(0);

        // Simple anonymous DAO implementation for test isolation
        mockUserDao = new UserDao() {
            @Override
            public Optional<User> findById(int id) {
                return id == testUser.getId() ? Optional.of(testUser) : Optional.empty();
            }

            @Override
            public Optional<User> findByUsername(String username) {
                return testUser.getUsername().equalsIgnoreCase(username) ? Optional.of(testUser) : Optional.empty();
            }

            @Override
            public java.util.List<User> findAll() { return java.util.List.of(testUser); }

            @Override
            public boolean create(User user) { return true; }

            @Override
            public boolean update(User user) { return true; }

            @Override
            public boolean updatePassword(int userId, String newPasswordHash, boolean mustChangePassword) {
                testUser.setPasswordHash(newPasswordHash);
                testUser.setMustChangePassword(mustChangePassword);
                return true;
            }

            @Override
            public boolean incrementFailedAttempts(int userId) {
                testUser.setFailedAttempts(testUser.getFailedAttempts() + 1);
                return true;
            }

            @Override
            public boolean resetFailedAttempts(int userId) {
                testUser.setFailedAttempts(0);
                return true;
            }

            @Override
            public boolean lockAccount(int userId) {
                testUser.setStatus("LOCKED");
                return true;
            }

            @Override
            public boolean unlockAccount(int userId) {
                testUser.setStatus("ACTIVE");
                testUser.setFailedAttempts(0);
                return true;
            }
        };

        authService = new AuthService(mockUserDao);
    }

    @Test
    @DisplayName("Successful login with correct credentials")
    void testLoginSuccess() {
        User loggedIn = authService.login("testadmin", "Password@123");
        assertNotNull(loggedIn);
        assertEquals("testadmin", loggedIn.getUsername());
        assertEquals("ADMIN", loggedIn.getRole());
    }

    @Test
    @DisplayName("Failed login with incorrect password increments failed attempts")
    void testLoginWrongPassword() {
        assertThrows(SecurityException.class, () -> authService.login("testadmin", "WrongPassword"));
        assertEquals(1, testUser.getFailedAttempts());
    }

    @Test
    @DisplayName("Account is locked after reaching maximum failed login attempts")
    void testAccountLockoutAfterMaxAttempts() {
        testUser.setFailedAttempts(4); // 4 prior failed attempts
        assertThrows(SecurityException.class, () -> authService.login("testadmin", "WrongPassword"));
        assertTrue(testUser.isLocked(), "User status should be LOCKED after 5 failed attempts");
    }

    @Test
    @DisplayName("Password change succeeds with valid current password")
    void testChangePasswordSuccess() {
        boolean changed = authService.changePassword(testUser.getId(), "Password@123", "NewSecret@999");
        assertTrue(changed);
        assertTrue(PasswordUtil.checkPassword("NewSecret@999", testUser.getPasswordHash()));
    }

    @Test
    @DisplayName("Password change fails when current password is wrong")
    void testChangePasswordWrongCurrent() {
        assertThrows(SecurityException.class, () -> 
            authService.changePassword(testUser.getId(), "WrongOld", "NewSecret@999")
        );
    }
}
