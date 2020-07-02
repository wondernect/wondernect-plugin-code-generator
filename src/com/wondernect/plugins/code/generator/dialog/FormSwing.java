package com.wondernect.plugins.code.generator.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.wondernect.plugins.code.generator.WondernectBaseCodeGenerator;
import com.wondernect.plugins.code.generator.WondernectBaseLongCodeGenerator;
import com.wondernect.plugins.code.generator.WondernectBaseStringCodeGenerator;

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
    // 0-BaseStringModel; 1-BaseLongModel; 2-BaseModel
    private int baseModelType;
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

    public FormSwing(int baseModelType, Project project, PsiFile psiFile) {
        this.baseModelType = baseModelType;
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
                // 0-BaseStringModel; 1-BaseLongModel; 2-BaseModel
                switch (baseModelType) {
                    case 0:
                    {
                        WondernectBaseStringCodeGenerator wondernectBaseStringCodeGenerator = new WondernectBaseStringCodeGenerator(project, psiFile, author, version, service);
                        wondernectBaseStringCodeGenerator.generateCode();
                        break;
                    }
                    case 1:
                    {
                        WondernectBaseLongCodeGenerator wondernectBaseLongCodeGenerator = new WondernectBaseLongCodeGenerator(project, psiFile, author, version, service);
                        wondernectBaseLongCodeGenerator.generateCode();
                        break;
                    }
                    case 2:
                    {
                        WondernectBaseCodeGenerator wondernectBaseCodeGenerator = new WondernectBaseCodeGenerator(project, psiFile, author, version, service);
                        wondernectBaseCodeGenerator.generateCode();
                        break;
                    }
                    default:
                    {
                        Messages.showMessageDialog(project, "BaseModel类型选择有误", "ERROR", Messages.getErrorIcon());
                        break;
                    }
                }
                r2.setText("执行完毕!!!");
            }
        });
        return south;
    }
}
