package com.wondernect.plugins.code.generator.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.wondernect.plugins.code.generator.WondernectCodeGenerator;

import javax.swing.*;
import java.awt.*;

/**
 * Copyright (C), 2020, wondernect.com
 * FileName: FormTestSwing
 * Author: chenxun
 * Date: 2020-06-21 23:15
 * Description:
 */
public class FormSwing {
    private Project project;
    private PsiFile psiFile;

    private JPanel north = new JPanel();
    private JPanel center = new JPanel();
    private JPanel south = new JPanel();
    //为了让位于底部的按钮可以拿到组件内容，这里把表单组件做成类属性
    private JLabel r1 = new JLabel("执行状态：");
    private JLabel r2 = new JLabel("");
    private JLabel author = new JLabel("作者：");
    private JTextField authorContent = new JTextField();
    private JLabel version = new JLabel("版本号：");
    private JTextField versionContent = new JTextField();
    private JLabel service = new JLabel("服务：");
    private JTextField serviceContent = new JTextField();

    public FormSwing(Project project, PsiFile psiFile) {
        this.project = project;
        this.psiFile = psiFile;
    }

    public JPanel initNorth() {
        //定义表单的标题部分，放置到IDEA会话框的顶部位置
        JLabel title = new JLabel("请输入以下信息");
        title.setFont(new Font("微软雅黑", Font.PLAIN, 26)); //字体样式
        title.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        title.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        north.add(title);
        return north;
    }

    public JPanel initCenter() {
        //定义表单的主体部分，放置到IDEA会话框的中央位置
        //一个简单的4行2列的表格布局
        center.setLayout(new GridLayout(4, 2));
        //row1：按钮事件触发后将结果打印在这里
        r1.setForeground(new Color(255, 47, 93)); //设置字体颜色
        center.add(r1);
        r2.setForeground(new Color(139, 181, 20)); //设置字体颜色
        center.add(r2);
        center.add(author);
        center.add(authorContent);
        center.add(version);
        center.add(versionContent);
        center.add(service);
        center.add(serviceContent);
        return center;
    }

    public JPanel initSouth() {
        //定义表单的提交按钮，放置到IDEA会话框的底部位置
        JButton submit = new JButton("执行");
        submit.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        submit.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        south.add(submit);
        //按钮事件绑定
        submit.addActionListener(e -> {
            String author = authorContent.getText();
            String version = versionContent.getText();
            String service = serviceContent.getText();
            if (author == null || "".equals(author.trim()) ||
                    version == null || "".equals(version.trim()) ||
                    service == null || "".equals(service.trim())) {
                Messages.showMessageDialog(project, "任一信息不能为空", "ERROR", Messages.getErrorIcon());
            } else {
                WondernectCodeGenerator wondernectCodeGenerator = new WondernectCodeGenerator(project, psiFile, author, version, service);
                wondernectCodeGenerator.generateCode();
                r2.setText("代码生成完毕!!!");
            }
        });
        return south;
    }
}
