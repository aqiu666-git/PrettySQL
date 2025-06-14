package com.chen.utils;

import com.chen.entity.DbConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static com.chen.constant.DbConstant.urlFix;
import static com.chen.constant.FileConstant.CONFIG_PATH;
import static com.chen.constant.MessageConstants.*;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/14 7:25
 */
public class DbConfigUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从项目目录中查找 YAML 配置并解析数据库连接参数，支持 spring.datasource 与 druid.master 配置
     */
    public static DbConfig tryLoadDbConfig(Project project) {
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


         if (dbConfig!=null){
             saveToCache(project, dbConfig);
         }

        return dbConfig;
    }


    /**
     * 递归查找所有 application*.yml 配置文件（仅限 src/main/resources）
     */
    public static List<File> findAllYmlFiles(File rootDir) {
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
    public static boolean isYmlFile(File file) {
        String name = file.getName();
        return name.matches("application(-[\\w]+)?\\.ya?ml") &&
                file.getParentFile() != null &&
                file.getParentFile().getPath().replace("\\", "/").endsWith("src/main/resources");
    }


    /**
     * 弹出异步输入框，直到用户输入完整数据库 URL 和用户名或取消
     *
     * @param project  当前项目
     * @param callback 用户输入完成回调
     */
    public static void promptUserInput(Project project, Consumer<DbConfig> callback) {
        ApplicationManager.getApplication().invokeLater(() -> {
            JTextField urlField = new JTextField("jdbc:mysql://127.0.0.1:3306/db");
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("数据库 URL:"));
            panel.add(urlField);
            panel.add(new JLabel("用户名:"));
            panel.add(usernameField);
            panel.add(new JLabel("密码:"));
            panel.add(passwordField);

            while (true) {
                int result = JOptionPane.showConfirmDialog(
                        null,
                        panel,
                        "请输入数据库配置",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result != JOptionPane.OK_OPTION) {
                    break;
                }

                String url = urlField.getText().trim();
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                List<String> errorList = new ArrayList<>();
                if (!StringUtils.notBlankAndNotNullStr(url)) {
                    errorList.add("数据库 URL");
                }
                if (!StringUtils.notBlankAndNotNullStr(username)) {
                    errorList.add("用户名");
                }

                if (errorList.isEmpty()) {
                    callback.accept(new DbConfig(url+urlFix, username, password));
                    break;
                } else {
                    Messages.showErrorDialog(project,
                            "以下字段不能为空：\n" + String.join("、", errorList),
                            "输入不完整");
                }
            }
        });
    }

    /**
     * 弹出同步阻塞输入框，直到用户输入完整数据库 URL 和用户名或取消
     *
     * @param project 当前项目
     * @return 用户输入的数据库配置，用户取消返回 null
     */
    public static DbConfig promptUserInputSync(Project project) {
        JTextField urlField = new JTextField("jdbc:mysql://127.0.0.1:3306/db");
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("数据库 URL:"));
        panel.add(urlField);
        panel.add(new JLabel("用户名:"));
        panel.add(usernameField);
        panel.add(new JLabel("密码:"));
        panel.add(passwordField);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "请输入数据库配置",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return null; // 用户取消
            }

            String url = urlField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            List<String> errorList = new ArrayList<>();
            if (!StringUtils.notBlankAndNotNullStr(url)) {
                errorList.add("数据库 URL");
            }
            if (!StringUtils.notBlankAndNotNullStr(username)) {
                errorList.add("用户名");
            }

            if (errorList.isEmpty()) {
                return new DbConfig(url + urlFix, username, password);
            } else {
                Messages.showErrorDialog(project,
                        "以下字段不能为空：\n" + String.join("、", errorList),
                        "输入不完整");
            }
        }
    }


    /**
     * 保存数据库配置到缓存文件，写成 JSON 格式，并测试数据库连接有效性
     *
     * @param project 当前项目
     * @param config  数据库配置对象
     */
    public static boolean saveToCache(Project project, DbConfig config) {
        Path path = Paths.get(project.getBasePath(), CONFIG_PATH);

        try {
            // 优先判断：如果已有缓存且内容一致，则跳过所有操作，提升性能
            if (Files.exists(path)) {
                String oldJson = Files.readString(path, StandardCharsets.UTF_8);
                DbConfig oldConfig = objectMapper.readValue(oldJson, DbConfig.class);
                if (Objects.equals(oldConfig, config)) {
                    // 配置一致，无需保存
                    return true;
                }
            }

            // 配置有变化，先测试连接
            if (config!=null && !JdbcTableInfoUtil.testConnection(config)) {
                Messages.showErrorDialog(project, SQL_ERROR_CONNECTION_FAIL, SQL_ERROR_TITLE_CONNECTION_FAIL);
                return false;
            }

            // 写入新配置
            Files.createDirectories(path.getParent());
            String json = objectMapper.writeValueAsString(config);
            Files.writeString(path, json, StandardCharsets.UTF_8);
            return true;

        } catch (IOException e) {
            Messages.showErrorDialog(project, CONFIG_SAVE_FAIL_PREFIX  + e.getMessage(), CONFIG_SAVE_FAIL_TITLE );
            return false;
        }
    }




    /**
     * 从缓存文件中读取数据库配置（JSON格式）
     *
     * @param project 当前项目
     * @return 配置对象，读取失败或文件不存在返回 null
     */
    public static DbConfig loadFromCache(Project project) {
        try {
            Path path = Paths.get(project.getBasePath(), CONFIG_PATH);
            if (!Files.exists(path)) return null;
            String json = Files.readString(path, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, DbConfig.class);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return null;
    }
}
