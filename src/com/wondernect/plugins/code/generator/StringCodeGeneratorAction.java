package com.wondernect.plugins.code.generator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.wondernect.plugins.code.generator.dialog.FormDialog;

/**
 * Created on 2020-06-20.
 * Esystem
 *
 * @author cxhome
 */
public class StringCodeGeneratorAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        assert project != null;
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        assert psiFile != null;

        FormDialog formTestDialog = new FormDialog(project, psiFile);
        //是否允许用户通过拖拽的方式扩大或缩小你的表单框，我这里定义为true，表示允许
        formTestDialog.setResizable(true);
        formTestDialog.show();
    }
}
