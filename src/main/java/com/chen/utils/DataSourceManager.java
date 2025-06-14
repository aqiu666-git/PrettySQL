package com.chen.utils;

import com.chen.entity.DbConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/14 8:27
 */
public class DataSourceManager {


    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static final Map<DbConfig, HikariDataSource> cache = new ConcurrentHashMap<>();

    public static DataSource getDataSource(DbConfig config) {
        return cache.computeIfAbsent(config, cfg -> {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(cfg.getUrl());
            hikariConfig.setUsername(cfg.getUsername());
            hikariConfig.setPassword(cfg.getPassword());
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(5000);
            hikariConfig.setIdleTimeout(30000);
            hikariConfig.setMaxLifetime(60000);
            return new HikariDataSource(hikariConfig);
        });
    }

}
