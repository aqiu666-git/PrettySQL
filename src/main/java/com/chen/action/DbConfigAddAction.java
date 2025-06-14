package com.chen.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.chen.constant.FileConstant.CONFIG_PATH;
import static com.chen.constant.MessageConstants.*;
import static com.chen.utils.DbConfigUtil.*;
import static com.chen.utils.JdbcTableInfoUtil.testConnection;

/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/14 10:23
 */
public class DbConfigAddAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent element) {
        Project project = element.getProject();
        if (project == null) {
            return;
        }
        promptUserAdd(element.getProject(), config -> {
            boolean state = testConnection(config);
            if (!state) {
                Messages.showErrorDialog(element.getProject(), SQL_ERROR_CONNECTION_FAIL, SQL_ERROR_TITLE_CONNECTION_FAIL);
                return;
            }
            if (saveToCache(element.getProject(), config)) {
                Path path = Paths.get(element.getProject().getBasePath(), CONFIG_PATH);
                Messages.showInfoMessage(
                        CONFIG_SAVE_SUCCESS_MESSAGE_PREFIX  + path.toString() + CONFIG_SAVE_SUCCESS_MESSAGE_SUFFIX,
                        CONFIG_SAVE_SUCCESS_TITLE
                );
            }
        });
    }
}

