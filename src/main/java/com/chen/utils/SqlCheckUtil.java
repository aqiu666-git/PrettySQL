package com.chen.utils;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/13 14:30
 */
public class SqlCheckUtil {

    /**
     * 检查SQL语法正确性
     * @param sql 需要检查的SQL
     * @return null表示无语法错误，否则返回错误信息
     */
    public static String checkSyntax(String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            return null;
        } catch (JSQLParserException e) {
            return "SQL语法错误: " + e.getMessage();
        }
    }

    /**
     * 检查危险SQL（如未加WHERE的DELETE/UPDATE）
     * @param sql 需要检查的SQL
     * @return 错误/警告信息，无问题返回null
     */
    public static String checkDangerous(String sql) {
        String lower = sql.trim().toLowerCase();
        if ((lower.startsWith("delete") || lower.startsWith("update")) && !lower.contains("where")) {
            return "警告：检测到未加 WHERE 的 DELETE/UPDATE 语句，可能会影响全表数据！";
        }
        return null;
    }
}
