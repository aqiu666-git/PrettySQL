package com.chen.constant;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/14 10:17
 */
public class DataSourceConstants {

    // ================== 驱动类名 ==================
    public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";

    // ================== 连接池配置 ==================
    public static final int MAX_POOL_SIZE = 5;              // 最大连接数
    public static final int MIN_IDLE = 1;                   // 最小空闲连接数
    public static final long CONNECTION_TIMEOUT = 5000L;    // 获取连接超时时间（ms）
    public static final long IDLE_TIMEOUT = 30000L;         // 空闲连接超时时间（ms）
    public static final long MAX_LIFETIME = 60000L;         // 最大连接生存时间（ms）

    // ================== 支持的数据库类型 ==================
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_ORACLE = "oracle";
}
