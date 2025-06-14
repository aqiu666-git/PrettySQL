package com.chen.utils;

import com.chen.constant.DataSourceConstants;
import com.chen.entity.ColumnMeta;
import com.chen.entity.DbConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static com.chen.utils.DbConfigUtil.parseDbType;

/**
 * JDBC 工具类，用于获取指定表的字段元数据信息
 * 包括字段名称、类型、是否主键和备注信息
 *
 * 依赖 MySQL 数据库驱动：com.mysql.cj.jdbc.Driver
 *
 * @author czh
 * @version 1.0
 * @date 2025/6/12 14:44
 */
public class JdbcTableInfoUtil {

    /**
     * 获取指定表的字段元数据列表
     *
     * @param dbConfig 数据库连接配置
     * @param tableName 表名
     * @return 字段元数据列表（ColumnMeta）
     * @throws ClassNotFoundException 如果 JDBC 驱动未加载
     */
    public static List<ColumnMeta> getTableColumnsFromMySQL(DbConfig dbConfig, String tableName) throws Exception {
        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {

            DatabaseMetaData meta = conn.getMetaData();

            String catalog = null;
            String url = dbConfig.getUrl();
            if (url != null) {
                int idx1 = url.indexOf("/", "jdbc:mysql://".length());
                int idx2 = url.indexOf("?", idx1);
                if (idx1 != -1) {
                    if (idx2 != -1) {
                        catalog = url.substring(idx1 + 1, idx2);
                    } else {
                        catalog = url.substring(idx1 + 1);
                    }
                }
            }

            // 获取表的主键字段集合
            Set<String> pkSet = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(catalog, null, tableName)) {
                while (pkRs.next()) {
                    pkSet.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // 获取表的字段信息
            List<ColumnMeta> columns = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(catalog, null, tableName, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");    // 字段名称
                    String type = rs.getString("TYPE_NAME");      // 字段类型
                    String remark = rs.getString("REMARKS");      // 字段注释
                    boolean pk = pkSet.contains(name);            // 是否为主键
                    columns.add(new ColumnMeta(name, type, pk, remark));
                }
            }

            return columns;

        }
    }
    public static List<ColumnMeta> getTableColumns(DbConfig dbConfig, String tableName) throws Exception {
        String dbType = parseDbType(dbConfig.getUrl());
        switch (dbType) {
            case "oracle":
                return getTableColumnsFromOracle(dbConfig, tableName);
            case "mysql":
                return getTableColumnsFromMySQL(dbConfig, tableName);
            default:
                throw new RuntimeException("不支持的数据库类型: " + dbType);
        }
    }
    public static List<ColumnMeta> getTableColumnsFromOracle(DbConfig dbConfig, String tableName) throws Exception {
        List<ColumnMeta> columns = new ArrayList<>();
        Set<String> pkSet = new HashSet<>();

        try (Connection conn = DataSourceManager.getDataSource(dbConfig).getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String schema = dbConfig.getUsername().toUpperCase();

            // 获取主键
            try (ResultSet pkRs = meta.getPrimaryKeys(null, schema, tableName.toUpperCase())) {
                while (pkRs.next()) {
                    pkSet.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            // 获取列及类型
            String columnSql = "SELECT col.COLUMN_NAME, col.DATA_TYPE, com.COMMENTS " +
                    "FROM ALL_TAB_COLUMNS col " +
                    "LEFT JOIN ALL_COL_COMMENTS com " +
                    "ON col.OWNER = com.OWNER AND col.TABLE_NAME = com.TABLE_NAME AND col.COLUMN_NAME = com.COLUMN_NAME " +
                    "WHERE col.OWNER = ? AND col.TABLE_NAME = ?";

            try (PreparedStatement ps = conn.prepareStatement(columnSql)) {
                ps.setString(1, schema);
                ps.setString(2, tableName.toUpperCase());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("COLUMN_NAME");
                        String type = rs.getString("DATA_TYPE");
                        String remark = rs.getString("COMMENTS");
                        boolean pk = pkSet.contains(name);
                        columns.add(new ColumnMeta(name, type, pk, remark));
                    }
                }
            }
        }

        return columns;
    }

    /**
     * 检查数据库连接是否有效
     *
     * @param dbConfig 数据库连接配置
     * @return true 表示连接成功，false 表示连接失败
     */
    public static boolean testConnection(DbConfig dbConfig) {
        try {
            String dbType = parseDbType(dbConfig.getUrl());

            // 加载对应数据库驱动类
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

            try (Connection conn = DriverManager.getConnection(
                    dbConfig.getUrl(),
                    dbConfig.getUsername(),
                    dbConfig.getPassword())) {
                return conn != null && !conn.isClosed();
            }

        } catch (Exception e) {
            return false;
        }
    }

}
