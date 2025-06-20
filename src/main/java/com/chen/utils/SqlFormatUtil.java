package com.chen.utils;


import com.github.vertical_blank.sqlformatter.SqlFormatter;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/12 8:28
 */
public class SqlFormatUtil {
    public static String formatSql(String sql, String dialect) {
        try {
            return SqlFormatter.of(dialect).format(sql);
        } catch (Exception e) {
            return sql;
        }
    }
}
