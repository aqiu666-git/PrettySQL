package com.chen.utils;

import com.chen.entity.ColumnMeta;
import com.chen.entity.DbConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public static List<ColumnMeta> getTableColumns(DbConfig dbConfig, String tableName) throws Exception {
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
    /**
     * 检查数据库连接是否有效
     *
     * @param dbConfig 数据库连接配置
     * @return true 表示连接成功，false 表示连接失败
     */
    public static boolean testConnection(DbConfig dbConfig) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
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
