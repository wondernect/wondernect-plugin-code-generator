package com.wondernect.plugins.code.generator;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.wondernect.plugins.code.generator.util.PsiStringUtils;
import com.wondernect.plugins.code.generator.util.PsiUtils;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Copyright (C), 2020, wondernect.com
 * FileName: WondernectCodeGenerator
 * Author: chenxun
 * Date: 2020-06-21 23:30
 * Description: wondernect code generator
 */
public class WondernectExcelExportItemHandlerCodeGenerator {

    private PsiDirectory workDir;
    private Map<String, PsiDirectory> directoryMap = new HashMap<>();

    private Project project;
    private PsiFile psiFile;
    private PsiDirectory containerDirectory;
    private String currentDirectory;
    private PsiUtils psiUtils;
    private Module module;
    private String author;
    private String version;
    private String service;

    public WondernectExcelExportItemHandlerCodeGenerator(Project project, PsiFile psiFile, String author, String version, String service) {
        this.project = project;
        this.psiFile = psiFile;
        this.author = author;
        this.version = version;
        this.service = service;
    }

    public void generateCode() {
        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        PsiClass[] classes = javaFile.getClasses();
        assert classes.length != 0;
        PsiClass psiClass = classes[0];
        assert psiClass != null;
        if (null == psiClass.getAnnotation("io.swagger.annotations.ApiModel")) {
            // 只处理被ApiModel注解的类
            Messages.showMessageDialog(project, "所选实体类非ApiModel注解类", "ERROR", Messages.getErrorIcon());
            return;
        }
        // 获取当前实体所在目录的上级目录
        containerDirectory = psiClass.getContainingFile().getContainingDirectory();
        currentDirectory = containerDirectory.getName();
        if (currentDirectory.contains("dto")) {
            currentDirectory = null;
        }
        workDir = containerDirectory.getParentDirectory();
        if (currentDirectory != null) {
            workDir = workDir.getParentDirectory();
        }
        module = FileIndexFacade.getInstance(project).getModuleForFile(classes[0].getContainingFile().getVirtualFile());
        psiUtils = PsiUtils.of(module);
        // 加载所有目录
        initDirs();
        EntityClass entityClass = new EntityClass(author, version, service);
        entityClass.setEntityClass(psiClass);
        // 获取实体相关信息
        PsiAnnotation apiModelAnnotation = entityClass.getEntityClass().getAnnotation("io.swagger.annotations.ApiModel");
        assert apiModelAnnotation != null;
        Optional<String> descriptionOptional = psiUtils.getAnnotationValue(apiModelAnnotation, "value");
        String description = null;
        if (descriptionOptional.isPresent() &&
                !"".equals(descriptionOptional.get()) &&
                !"\"\"".equals(descriptionOptional.get())) {
            description = descriptionOptional.get();
        } else {
            descriptionOptional = psiUtils.getAnnotationValue(apiModelAnnotation, "description");
            if (descriptionOptional.isPresent()) {
                description = descriptionOptional.get();
            }
        }
        assert description != null;
        description = description.replace("\"", "");
        entityClass.setEntityDescription(description);
        // 构造excel item
        getFieldsItem(entityClass);
        // 创建excel item handler
        WriteCommandAction.runWriteCommandAction(project, () -> createExcelExportItemHandler(entityClass));
    }

    /**
     * 初始化所有文件夹
     */
    private void initDirs() {
        List<String> directories = Arrays.asList("excel_export");
        directoryMap.clear();
        directories.forEach(dir -> {
            // 创建1级目录
            PsiDirectory directory = workDir.findSubdirectory(dir);
            if (null == directory) {
                directory = workDir.createSubdirectory(dir);
            }
            directoryMap.put(dir, directory);
            // 如果需要则创建2级目录
            if (currentDirectory != null) {
                dir = dir + "/" + currentDirectory;
                String[] dirs = dir.split("/");
                PsiDirectory subdirectory = workDir.findSubdirectory(dirs[0]);
                PsiDirectory subDir = subdirectory.findSubdirectory(dirs[1]);
                if (null == subDir) {
                    subDir = subdirectory.createSubdirectory(dirs[1]);
                }
                directoryMap.put(dir, subDir);
            }
        });
    }

    /**
     * 创建excel export item handler
     */
    private void createExcelExportItemHandler(EntityClass entityClass) {
        String dir = currentDirectory == null ? "excel_export" : "excel_export/" + currentDirectory;
        PsiDirectory excelExportItemDirectory = directoryMap.get(dir);
        if (entityClass.getResponseFields() != null && entityClass.getResponseFields().size() > 0) {
            for (String itemName : entityClass.getResponseFields().keySet()) {
                String itemType = entityClass.getResponseFields().get(itemName);
                String description = entityClass.getResponseFieldsDescription().get(itemName);
                String excelExportItemHandlerName = entityClass.getEntityName() + PsiStringUtils.firstLetterToUpper(itemName) + "ExportHandler";
                ClassCreator.of(module).init(
                        excelExportItemHandlerName,
                        getCommentContent(description + "导出item handler", entityClass.getAuthor()) +
                                "\n@Service" +
                                "\npublic class " + excelExportItemHandlerName + " implements ESExcelItemHandler<" + itemType + "> {\n" +

                                "\n@Override" +
                                "\npublic String itemName() {" +
                                "\nreturn \"" + itemName + "\";" +
                                "}" +

                                "\n@Override" +
                                "\npublic String itemTitle() {" +
                                "\nreturn \"" + description + "\";" +
                                "}" +

                                "\n@Override" +
                                "\npublic int itemOrder() {" +
                                "\nreturn 0;" +
                                "}" +

                                "\n@Override" +
                                "\npublic Boolean hidden() {" +
                                "\nreturn false;" +
                                "}" +

                                "\n@Override" +
                                "\npublic Object handleExcelExportItemObject(" + itemType + " object) {" +
                                "\nreturn object;" +
                                "}" +

                                "}"
                )
                        .importClass("com.wondernect.elements.easyoffice.excel.ESExcelItemHandler")
                        .importClass("org.springframework.stereotype.Service")
                        .addTo(excelExportItemDirectory);
            }
        }
    }

    private String getCommentContent(String desc, String author) {
        return "/** " + desc +
                " \n * @author " + author + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                " **/";
    }

    private void getFieldsItem(EntityClass entityClass) {
        for (PsiField field : entityClass.getEntityClass().getFields()) {
            String name = field.getName();
            PsiType type = field.getType();
            String typeName = type.getCanonicalText();
            if (typeName.contains(".")) {
                typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
            }
            // @ApiModelProperty注解
            PsiAnnotation fieldAnnotation = field.getAnnotation("io.swagger.annotations.ApiModelProperty");
            Optional<String> descriptionOptional = psiUtils.getAnnotationValue(fieldAnnotation, "value");
            String description = null;
            if (descriptionOptional.isPresent() &&
                    !"".equals(descriptionOptional.get()) &&
                    !"\"\"".equals(descriptionOptional.get())) {
                description = descriptionOptional.get();
            } else {
                descriptionOptional = psiUtils.getAnnotationValue(fieldAnnotation, "notes");
                if (descriptionOptional.isPresent()) {
                    description = descriptionOptional.get();
                }
            }
            assert description != null;
            description = description.replace("\"", "");

            entityClass.getResponseFields().put(name, typeName);
            entityClass.getResponseFieldsDescription().put(name, description);
        }
    }
}
