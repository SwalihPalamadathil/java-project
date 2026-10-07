package com.shop.util;

import com.shop.model.User;
import java.time.LocalDateTime;

/**
 * Singleton managing user session state across application screens.
 */
public final class SessionManager {

    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime loginTime;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
    }

    public void logout() {
        this.currentUser = null;
        this.loginTime = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : "SYSTEM";
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }
}
