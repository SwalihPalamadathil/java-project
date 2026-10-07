package com.shop.model;

import java.time.LocalDateTime;

/**
 * POJO representing an application user (Admin or Staff/Cashier).
 */
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // 'ADMIN' or 'STAFF'
    private String status; // 'ACTIVE', 'LOCKED', 'INACTIVE'
    private int failedAttempts;
    private boolean mustChangePassword;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(int id, String username, String passwordHash, String fullName, String role, 
                String status, int failedAttempts, boolean mustChangePassword, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.mustChangePassword = mustChangePassword;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    public boolean isLocked() {
        return "LOCKED".equalsIgnoreCase(this.status);
    }

    @Override
    public String toString() {
        return fullName != null ? fullName + " (" + role + ")" : username;
    }
}
