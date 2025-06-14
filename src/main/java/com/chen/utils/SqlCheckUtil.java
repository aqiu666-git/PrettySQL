package com.chen.utils;
import com.chen.constant.SqlConstants;
import com.chen.entity.DbConfig;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * SQL 语句检查工具类
 */
public class SqlCheckUtil {

    public static String checkSQLWithRollback(DbConfig config, String sql) {
        try (Connection conn = DataSourceManager.getDataSource(config).getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            stmt.execute(sql);
            conn.rollback();
            return null;

        } catch (Exception e) {
            return SqlConstants.ERROR_SQL_EXECUTE_PREFIX + e.getMessage();
        }
    }

    @Deprecated
    private static String checkSyntaxOnly(String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            return null;
        } catch (JSQLParserException e) {
            return "SQL语法错误: " + e.getMessage();
        }
    }


    public static String checkDangerous(String sql) {
        String lower = sql.trim().toLowerCase();
        if ((lower.startsWith(SqlConstants.SQL_DELETE) || lower.startsWith(SqlConstants.SQL_UPDATE))
                && !lower.contains(SqlConstants.SQL_WHERE)) {
            return SqlConstants.WARN_UNSAFE_DELETE_UPDATE;
        }
        return null;
    }
}
