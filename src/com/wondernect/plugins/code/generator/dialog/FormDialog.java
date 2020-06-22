package com.wondernect.plugins.code.generator.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Copyright (C), 2020, wondernect.com
 * FileName: FormTestDialog
 * Author: chenxun
 * Date: 2020-06-21 23:15
 * Description:
 */
public class FormDialog extends DialogWrapper {

    private FormSwing formTestSwing;

    public FormDialog(@Nullable Project project, @Nullable PsiFile psiFile) {
        super(true);
        assert project != null;
        assert psiFile != null;
        // 获取到当前项目的名称设置会话框标题
        setTitle(project.getName());
        formTestSwing = new FormSwing(project, psiFile);
        //触发一下init方法，否则swing样式将无法展示在会话框
        init();
    }

    @Override
    protected JComponent createNorthPanel() {
        return formTestSwing.initNorth();
    }

    @Override
    protected JComponent createSouthPanel() {
        return formTestSwing.initSouth();
    }

    @Override
    protected JComponent createCenterPanel() {
        return formTestSwing.initCenter();
    }
}
