package com.shop.dao;

import com.shop.model.Setting;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object interface for Key-Value application settings.
 */
public interface SettingDao {
    Optional<String> get(String key);
    Map<String, String> getAll();
    boolean set(String key, String value, String description);
}
