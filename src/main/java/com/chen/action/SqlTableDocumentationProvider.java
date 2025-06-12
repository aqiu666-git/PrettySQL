package com.chen.action;

import com.chen.entity.ColumnMeta;
import com.chen.entity.DbConfig;
import com.chen.utils.JdbcTableInfoUtil;
import com.chen.utils.StringUtils;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 表结构文档展示提供器：支持在 IntelliJ 中对 SQL 中的表名悬停显示结构
 * 自动从 application.yml / application-dev.yml 中提取数据库连接配置并通过 JDBC 获取表字段元数据
 */
public class SqlTableDocumentationProvider implements DocumentationProvider {

    /**
     * 悬停展示文档：从 PSI 中识别表名，读取数据库连接并生成 HTML 表格
     */
    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        String tableName = extractTableName(element);
        if (tableName == null) {
            return null;
        }

        DbConfig dbConfig = tryLoadDbConfig(element.getProject());
        if (dbConfig == null) {
            return "<b>异常信息：</b> " + tableName + "<br>未找到数据库连接配置。";
        }

        List<ColumnMeta> columns;
        try {
            columns = JdbcTableInfoUtil.getTableColumns(dbConfig, tableName);
        } catch (Exception e) {
            return "<b>异常信息：</b> " + tableName + e.getMessage();
        }

        if (columns == null || columns.isEmpty()) {
            return "<b>异常信息：</b> " + tableName + "<br>未在数据库中找到该表结构或表无字段。";
        }

        return buildHtmlTable(tableName, columns);
    }

    @Override
    public @Nullable List<String> getUrlFor(PsiElement element, @Nullable PsiElement originalElement) {
        return null;
    }

    /**
     * 构建表结构的 HTML 表格展示
     */
    private String buildHtmlTable(String tableName, List<ColumnMeta> columns) {
        StringBuilder html = new StringBuilder();
        html.append("<style>")
                .append(".db-table { width:100%; border-collapse:collapse; font-family:'Segoe UI',Arial,sans-serif; background:#23272f; border-radius:10px; overflow:hidden; box-shadow:0 2px 12px rgba(0,0,0,0.40); }")
                .append(".db-table th, .db-table td { border:1px solid #444b58; padding:10px 8px; color:#d8dee9; text-align:center; vertical-align:middle; }")
                .append(".db-table th { background:linear-gradient(90deg,#34495e 0%,#23272f 100%); font-weight:600; font-size:15px; }")
                .append(".db-table td { background:#23272f; font-size:14px; }")
                .append(".db-table tr:hover td { background:#2d333b; }")
                .append(".db-table .pk { color:#ffb300; font-weight:bold; }")
                .append("</style>");

        html.append("<b style='font-size:1.1em;color:#e1eaff;'>表名：</b> ").append(tableName).append("<br>");
        html.append("<table class='db-table'><thead><tr>")
                .append("<th style='width: 25%;'>字段名称</th>")
                .append("<th style='width: 20%;'>类型</th>")
                .append("<th style='width: 40%;'>备注</th>")
                .append("<th style='width: 15%;'>主键</th>")
                .append("</tr></thead><tbody>");

        for (ColumnMeta col : columns) {
            html.append("<tr><td>").append(col.getName()).append("</td>")
                    .append("<td>").append(col.getType()).append("</td>")
                    .append("<td>").append(col.getRemark() == null ? "" : col.getRemark()).append("</td>")
                    .append("<td>").append(col.isPrimaryKey() ? "<span class='pk'>YES</span>" : "").append("</td></tr>");
        }

        html.append("</tbody></table>");
        return html.toString();
    }

    /**
     * 提取 SQL 中的表名，适配 MyBatis XML 或 SQL 字符串
     */
    private @Nullable String extractTableName(PsiElement element) {
        if (element == null) return null;
        String text = element.getText();
        if (text == null || !text.matches("[a-zA-Z_][a-zA-Z0-9_]*")) return null;

        PsiElement parent = element.getParent();

        // 支持 from/join 关键字场景
        if (parent != null && parent.getText().matches("(?i).*\\b(from|join)\\b.*" + text + ".*")) {
            return text;
        }

        // XML/SQL 中的 token 内容匹配
        if (element instanceof XmlToken || element instanceof XmlText) {
            Pattern pattern = Pattern.compile("\\b(from|join)\\s+([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(2);
            }
        }

        return text;
    }


    /**
     * 从项目目录中查找 YAML 配置并解析数据库连接参数，支持 spring.datasource 与 druid.master 配置
     */
    private DbConfig tryLoadDbConfig(Project project) {
        List<File> ymlFiles = findAllYmlFiles(new File(project.getBasePath()));
        DbConfig dbConfig = null;

        for (File yml : ymlFiles) {
            try (InputStream input = new FileInputStream(yml)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);
                if (data == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> spring = (Map<String, Object>) data.get("spring");
                if (spring == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
                if (datasource == null) continue;

                // 第一优先：spring.datasource 配置
                String url = String.valueOf(datasource.get("url"));
                String username = String.valueOf(datasource.get("username"));
                String password = String.valueOf(datasource.get("username"));
                System.out.println(password);

                if (StringUtils.notBlankAndNotNullStr(url) && StringUtils.notBlankAndNotNullStr(username)) {
                    dbConfig = new DbConfig(url, username, password);
                    break;
                }

                // 第二优先：druid.master 配置
                @SuppressWarnings("unchecked")
                Map<String, Object> druid = (Map<String, Object>) datasource.get("druid");
                if (druid == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> master = (Map<String, Object>) druid.get("master");
                if (master == null) continue;

                url = String.valueOf(master.get("url"));
                username = String.valueOf(master.get("username"));
                password = String.valueOf(master.get("password"));

                if (StringUtils.notBlankAndNotNullStr(url) && StringUtils.notBlankAndNotNullStr(username)) {
                    dbConfig = new DbConfig(url, username, password);
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        return dbConfig;
    }


    /**
     * 递归查找所有 application*.yml 配置文件（仅限 src/main/resources）
     */
    private List<File> findAllYmlFiles(File rootDir) {
        List<File> result = new ArrayList<>();
        Queue<File> queue = new LinkedList<>();
        queue.add(rootDir);

        while (!queue.isEmpty()) {
            File current = queue.poll();
            if (current == null || !current.exists()) continue;
            if (current.isDirectory()) {
                File[] children = current.listFiles();
                if (children != null) Collections.addAll(queue, children);
            } else if (isYmlFile(current)) {
                result.add(current);
            }
        }
        return result;
    }

    /**
     * 判断是否为 application(-xxx)?.yml 文件
     */
    private boolean isYmlFile(File file) {
        String name = file.getName();
        return name.matches("application(-[\\w]+)?\\.ya?ml") &&
                file.getParentFile() != null &&
                file.getParentFile().getPath().replace("\\", "/").endsWith("src/main/resources");
    }


}
