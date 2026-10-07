package com.shop.dao;

import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation of SettingDao.
 */
public class SettingDaoImpl implements SettingDao {

    private static final Logger logger = LoggerFactory.getLogger(SettingDaoImpl.class);

    @Override
    public Optional<String> get(String key) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("setting_value"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving setting {}: {}", key, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> map = new LinkedHashMap<>();
        String sql = "SELECT setting_key, setting_value FROM settings";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all settings: {}", e.getMessage(), e);
        }
        return map;
    }

    @Override
    public boolean set(String key, String value, String description) {
        String sql = "INSERT INTO settings (setting_key, setting_value, description) VALUES (?, ?, ?) "
                + "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value, description = excluded.description";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error setting configuration {}: {}", key, e.getMessage(), e);
        }
        return false;
    }
}
