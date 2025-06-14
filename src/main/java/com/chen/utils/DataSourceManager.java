package com.chen.utils;

import com.chen.constant.DataSourceConstants;
import com.chen.entity.DbConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.chen.utils.DbConfigUtil.parseDbType;

/**
 * 数据源管理类，基于 HikariCP 实现连接池缓存和管理。
 */
public class DataSourceManager {

    private static final Map<DbConfig, HikariDataSource> cache = new ConcurrentHashMap<>();

    public static DataSource getDataSource(DbConfig config) {
        return cache.computeIfAbsent(config, cfg -> {
            loadJdbcDriver(cfg.getUrl());
            return createHikariDataSource(cfg);
        });
    }

    private static void loadJdbcDriver(String url) {
        String dbType = parseDbType(url);
        try {
            switch (dbType) {
                case DataSourceConstants.DB_TYPE_MYSQL:
                    Class.forName(DataSourceConstants.MYSQL_DRIVER);
                    break;
                case DataSourceConstants.DB_TYPE_ORACLE:
                    Class.forName(DataSourceConstants.ORACLE_DRIVER);
                    break;
                default:
                    throw new RuntimeException("不支持的数据库类型: " + dbType);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC驱动加载失败: " + e.getMessage(), e);
        }
    }


    private static HikariDataSource createHikariDataSource(DbConfig cfg) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(cfg.getUrl());
        hikariConfig.setUsername(cfg.getUsername());
        hikariConfig.setPassword(cfg.getPassword());
        hikariConfig.setMaximumPoolSize(DataSourceConstants.MAX_POOL_SIZE);
        hikariConfig.setMinimumIdle(DataSourceConstants.MIN_IDLE);
        hikariConfig.setConnectionTimeout(DataSourceConstants.CONNECTION_TIMEOUT);
        hikariConfig.setIdleTimeout(DataSourceConstants.IDLE_TIMEOUT);
        hikariConfig.setMaxLifetime(DataSourceConstants.MAX_LIFETIME);
        return new HikariDataSource(hikariConfig);
    }
}
