package com.chen.action;

import com.chen.utils.SqlCheckUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;


/**
 * @author czh
 * @version 1.0
 * @description:
 * @date 2025/6/13 14:29
 */

public class CheckSqlAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);

        if (editor == null) {
            Messages.showWarningDialog(project, "请在编辑器中选中或输入SQL语句", "SQL检查");
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String sql = selectionModel.getSelectedText();
        if (sql == null || sql.trim().isEmpty()) {
            sql = editor.getDocument().getText();
        }

        if (sql == null || sql.trim().isEmpty()) {
            Messages.showWarningDialog(project, "未检测到SQL语句", "SQL检查");
            return;
        }

        // 语法检查
        String syntaxResult = SqlCheckUtil.checkSyntax(sql);
        if (syntaxResult != null) {
            Messages.showErrorDialog(project, syntaxResult, "SQL语法检查");
            return;
        }

        // 危险SQL检查
        String dangerResult = SqlCheckUtil.checkDangerous(sql);
        if (dangerResult != null) {
            Messages.showWarningDialog(project, dangerResult, "SQL危险检测");
            return;
        }

        Messages.showInfoMessage(project, "SQL语句语法正确", "SQL检查");
    }
}
