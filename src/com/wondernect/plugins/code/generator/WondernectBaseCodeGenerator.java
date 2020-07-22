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
public class WondernectBaseCodeGenerator {

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

    public WondernectBaseCodeGenerator(Project project, PsiFile psiFile, String author, String version, String service) {
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
        if (null == psiClass.getAnnotation("javax.persistence.Entity")) {
            // 只处理被Entity注解的类
            Messages.showMessageDialog(project, "所选实体类非Entity注解类", "ERROR", Messages.getErrorIcon());
            return;
        }
        // 获取当前实体所在目录的上级目录
        containerDirectory = psiClass.getContainingFile().getContainingDirectory();
        currentDirectory = containerDirectory.getName();
        if (currentDirectory.contains("model") || currentDirectory.contains("entity")) {
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
        // 获取id主键类型
        boolean result = generateEntityIdField(entityClass);
        assert result;
        // 获取实体相关信息
        PsiAnnotation apiModelAnnotation = entityClass.getEntityClass().getAnnotation("io.swagger.annotations.ApiModel");
        assert apiModelAnnotation != null;
        Optional<String> descriptionOptional = psiUtils.getAnnotationValue(apiModelAnnotation, "value");
        String description = null;
        if (descriptionOptional.isPresent()) {
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
        // 创建Repository
        WriteCommandAction.runWriteCommandAction(project, () -> createRepository(entityClass));
        // 创建Dao
        WriteCommandAction.runWriteCommandAction(project, () -> createDao(entityClass));
        // 创建Manager
        WriteCommandAction.runWriteCommandAction(project, () -> createManager(entityClass));
        // 创建requestDTO
        WriteCommandAction.runWriteCommandAction(project, () -> createRequestDTO(entityClass));
        // 创建responseDTO
        WriteCommandAction.runWriteCommandAction(project, () -> createResponseDTO(entityClass));
        // 创建listRequestDTO
        WriteCommandAction.runWriteCommandAction(project, () -> createListRequestDTO(entityClass));
        // 创建pageRequestDTO
        WriteCommandAction.runWriteCommandAction(project, () -> createPageRequestDTO(entityClass));
        // 创建service interface
        WriteCommandAction.runWriteCommandAction(project, () -> createServiceInterface(entityClass));
        // 创建service abstract
        WriteCommandAction.runWriteCommandAction(project, () -> createServiceAbstract(entityClass));
        // 创建service
        WriteCommandAction.runWriteCommandAction(project, () -> createService(entityClass));
        // 创建controller
        WriteCommandAction.runWriteCommandAction(project, () -> createController(entityClass));
    }

    /**
     * 初始化所有文件夹
     */
    private void initDirs() {
        List<String> directories = Arrays.asList("repository", "dao", "manager", "dto", "service", "controller");
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
     * 创建Repository
     */
    private void createRepository(EntityClass entityClass) {
        String dir = currentDirectory == null ? "repository" : "repository/" + currentDirectory;
        PsiDirectory repositoryDirectory = directoryMap.get(dir);
        String repositoryName = entityClass.getEntityName().concat("Repository");
        getBaseClass(
                "BaseRepository",
                baseStringRepositoryClass -> ClassCreator.of(module).init(repositoryName,
                        getCommentContent(entityClass.getEntityDescription() + "数据库操作类", entityClass.getAuthor()) +
                                "\npublic interface " + repositoryName + " extends BaseRepository<" + entityClass.getEntityName() + ", " + entityClass.getEntityIdType() + "> {}")
                        .importClass(entityClass.getEntityClass())
                        .importClass(baseStringRepositoryClass)
                        .addTo(repositoryDirectory)
        );
        entityClass.setRepositoryName(repositoryName);
    }

    /**
     * 创建Dao
     */
    private void createDao(EntityClass entityClass) {
        String dir = currentDirectory == null ? "dao" : "dao/" + currentDirectory;
        PsiDirectory daoDirectory = directoryMap.get(dir);
        String daoName = entityClass.getEntityName().concat("Dao");
        getBaseClass(
                "BaseDao",
                baseStringDaoClass -> ClassCreator.of(module).init(daoName,
                        getCommentContent(entityClass.getEntityDescription() + "数据库操作类", entityClass.getAuthor()) +
                                "\n@Repository \npublic class " + daoName + " extends BaseDao<" + entityClass.getEntityName() + ", " + entityClass.getEntityIdType() + "> {}")
                        .importClass("org.springframework.stereotype.Repository")
                        .importClass(entityClass.getEntityClass())
                        .importClass(baseStringDaoClass)
                        .addTo(daoDirectory)
        );
        entityClass.setDaoName(daoName);
    }

    /**
     * 创建Manager
     */
    private void createManager(EntityClass entityClass) {
        String dir = currentDirectory == null ? "manager" : "manager/" + currentDirectory;
        PsiDirectory managerDirectory = directoryMap.get(dir);
        String managerName = entityClass.getEntityName().replace("Entity", "").concat("Manager");
        getBaseClass(
                "BaseManager",
                baseStringManagerClass -> ClassCreator.of(module).init(
                        managerName,
                        getCommentContent(entityClass.getEntityDescription() + "服务操作类", entityClass.getAuthor()) +
                                "\n@Service \npublic class " + managerName + " extends BaseManager<" + entityClass.getEntityName() + ", " + entityClass.getEntityIdType() + "> {}"
                )
                        .importClass("org.springframework.stereotype.Service")
                        .importClass(entityClass.getEntityClass())
                        .importClass(baseStringManagerClass)
                        .addTo(managerDirectory)
        );
        entityClass.setManagerName(managerName);
    }

    /**
     * 创建RequestDTO
     */
    private void createRequestDTO(EntityClass entityClass) {
        String dir = currentDirectory == null ? "dto" : "dto/" + currentDirectory;
        PsiDirectory dtoDirectory = directoryMap.get(dir);
        String requestDTOName = "Save" + entityClass.getEntityName() + "RequestDTO";
        String requestDTODesc = entityClass.getEntityDescription() + "请求对象";

        ClassCreator.of(module).init(
                requestDTOName,
                getCommentContent(entityClass.getEntityDescription() + "请求DTO", entityClass.getAuthor()) +
                        "\n@Data" +
                        "\n@NoArgsConstructor" +
                        "\n@AllArgsConstructor" +
                        "\n@ApiModel(value = \""+ requestDTODesc +"\")" +
                        "\npublic class " + requestDTOName + " {" +
                        "\n" + getFieldsContentForREQDTO(entityClass) +
                        "}"
        )
                .importClass("lombok.Data")
                .importClass("lombok.NoArgsConstructor")
                .importClass("lombok.AllArgsConstructor")
                .importClass("io.swagger.annotations.ApiModel")
                .importClass("io.swagger.annotations.ApiModelProperty")
                .importClass("com.fasterxml.jackson.annotation.JsonProperty")
                .importClass("org.hibernate.validator.constraints.Length")
                .importClass("javax.validation.constraints.NotBlank")
                .importClass("javax.validation.constraints.NotNull")
                .importClass("com.fasterxml.jackson.annotation.JsonFormat")
                .addTo(dtoDirectory);
        entityClass.setRequestDTOName(requestDTOName);
    }

    /**
     * 创建ResponseDTO
     */
    private void createResponseDTO(EntityClass entityClass) {
        String dir = currentDirectory == null ? "dto" : "dto/" + currentDirectory;
        PsiDirectory dtoDirectory = directoryMap.get(dir);
        String responseDTOName = entityClass.getEntityName() + "ResponseDTO";
        String responseDTODesc = entityClass.getEntityDescription() + "响应对象";
        ClassCreator.of(module).init(
                responseDTOName,
                getCommentContent(entityClass.getEntityDescription() + "响应DTO", entityClass.getAuthor()) +
                        "\n@Data" +
                        "\n@NoArgsConstructor" +
                        "\n@AllArgsConstructor" +
                        "\n@ApiModel(value = \""+ responseDTODesc +"\")" +
                        "\npublic class " + responseDTOName + " {" +
                        "\n" + getFieldsContentForRESDTO(entityClass) +
                        "}"
        )
                .importClass("lombok.Data")
                .importClass("lombok.NoArgsConstructor")
                .importClass("lombok.AllArgsConstructor")
                .importClass("io.swagger.annotations.ApiModel")
                .importClass("io.swagger.annotations.ApiModelProperty")
                .importClass("com.fasterxml.jackson.annotation.JsonProperty")
                .addTo(dtoDirectory);
        entityClass.setResponseDTOName(responseDTOName);
    }

    /**
     * 创建ListRequestDTO
     */
    private void createListRequestDTO(EntityClass entityClass) {
        String dir = currentDirectory == null ? "dto" : "dto/" + currentDirectory;
        PsiDirectory dtoDirectory = directoryMap.get(dir);
        String listRequestDTOName = "List" + entityClass.getEntityName() + "RequestDTO";
        String listRequestDTODesc = entityClass.getEntityDescription() + "列表请求对象";
        ClassCreator.of(module).init(
                listRequestDTOName,
                getCommentContent(entityClass.getEntityDescription() + "列表请求DTO", entityClass.getAuthor()) +
                        "\n@Data" +
                        "\n@NoArgsConstructor" +
                        "\n@AllArgsConstructor" +
                        "\n@ApiModel(value = \""+ listRequestDTODesc +"\")" +
                        "\npublic class " + listRequestDTOName + " {\n" +
                        "\n@JsonProperty(\"sort_data_list\")" +
                        "\n@ApiModelProperty(notes = \"列表请求参数\")" +
                        "\nprivate List<SortData> sortDataList;" +
                        "\n}"
        )
                .importClass("lombok.Data")
                .importClass("lombok.NoArgsConstructor")
                .importClass("lombok.AllArgsConstructor")
                .importClass("io.swagger.annotations.ApiModel")
                .importClass("io.swagger.annotations.ApiModelProperty")
                .importClass("com.fasterxml.jackson.annotation.JsonProperty")
                .importClass("com.wondernect.elements.rdb.request.SortData")
                .importClass("java.util.List")
                .addTo(dtoDirectory);
        entityClass.setListRequestDTOName(listRequestDTOName);
    }

    /**
     * 创建PageRequestDTO
     */
    private void createPageRequestDTO(EntityClass entityClass) {
        String dir = currentDirectory == null ? "dto" : "dto/" + currentDirectory;
        PsiDirectory dtoDirectory = directoryMap.get(dir);
        String pageRequestDTOName = "Page" + entityClass.getEntityName() + "RequestDTO";
        String pageRequestDTODesc = entityClass.getEntityDescription() + "分页请求对象";
        ClassCreator.of(module).init(
                pageRequestDTOName,
                getCommentContent(entityClass.getEntityDescription() + "分页请求DTO", entityClass.getAuthor()) +
                        "\n@Data" +
                        "\n@NoArgsConstructor" +
                        "\n@AllArgsConstructor" +
                        "\n@ApiModel(value = \""+ pageRequestDTODesc +"\")" +
                        "\npublic class " + pageRequestDTOName + " {\n" +
                        "\n@NotNull(message = \"分页请求参数不能为空\")" +
                        "\n@JsonProperty(\"page_request_data\")" +
                        "\n@ApiModelProperty(notes = \"分页请求参数\")" +
                        "\nprivate PageRequestData pageRequestData;" +
                        "\n}"
        )
                .importClass("lombok.Data")
                .importClass("lombok.NoArgsConstructor")
                .importClass("lombok.AllArgsConstructor")
                .importClass("io.swagger.annotations.ApiModel")
                .importClass("io.swagger.annotations.ApiModelProperty")
                .importClass("com.fasterxml.jackson.annotation.JsonProperty")
                .importClass("com.wondernect.elements.rdb.request.PageRequestData")
                .importClass("javax.validation.constraints.NotNull")
                .addTo(dtoDirectory);
        entityClass.setPageRequestDTOName(pageRequestDTOName);
    }

    /**
     * 创建Service接口
     */
    private void createServiceInterface(EntityClass entityClass) {
        String dir = currentDirectory == null ? "service" : "service/" + currentDirectory;
        PsiDirectory serviceInterfaceDirectory = directoryMap.get(dir);
        String serviceInterfaceName = entityClass.getEntityName().concat("Interface");
        String content = getCommentContent(entityClass.getEntityDescription() + "服务接口类", entityClass.getAuthor()) +
                "\npublic interface " + serviceInterfaceName + " {\n" +

                "\n/** " +
                "\n * 创建" +
                "\n**/" +
                "\n" + entityClass.getResponseDTOName() + " create(" + entityClass.getRequestDTOName() + " " + entityClass.getRequestDTOVariableName() + "); " +

                "\n/** " +
                "\n * 更新" +
                "\n**/" +
                "\n" + entityClass.getResponseDTOName() + " update(" + entityClass.getEntityIdType() + " " + entityClass.getEntityIdName() + ", " + entityClass.getRequestDTOName() + " " + entityClass.getRequestDTOVariableName() + "); " +

                "\n/** " +
                "\n * 删除" +
                "\n**/" +
                "\nvoid deleteById(" + entityClass.getEntityIdType() + " " + entityClass.getEntityIdName() + ");" +

                "\n/** " +
                "\n * 获取详细信息" +
                "\n**/" +
                "\n" + entityClass.getResponseDTOName() + " findById(" + entityClass.getEntityIdType() + " " + entityClass.getEntityIdName() + "); " +

                "\n/** " +
                "\n * 列表" +
                "\n**/" +
                "\nList<" + entityClass.getResponseDTOName() + "> list(" + entityClass.getListRequestDTOName() + " " + entityClass.getListRequestDTOVariableName() + "); " +

                "\n/** " +
                "\n * 分页" +
                "\n**/" +
                "\nPageResponseData<" + entityClass.getResponseDTOName() + "> page(" + entityClass.getPageRequestDTOName() + " " + entityClass.getPageRequestDTOVariableName() + "); " +

                "\n/** " +
                "\n * 获取excel的所有可用列名、类型、描述、get方法、set方法" +
                "\n**/" +
                "\nList<ESExcelItem> excelItemList(); " +

                "\n/** " +
                "\n * excel导出" +
                "\n**/" +
                "\nvoid excelDataExport(" + entityClass.getListRequestDTOName() + " " + entityClass.getListRequestDTOVariableName() + ", HttpServletRequest request, HttpServletResponse response); " +

                "\n}";
        ClassCreator.of(module).init(serviceInterfaceName, content)
                .importClass(entityClass.getRequestDTOName())
                .importClass(entityClass.getResponseDTOName())
                .importClass(entityClass.getListRequestDTOName())
                .importClass(entityClass.getPageRequestDTOName())
                .importClass("java.util.List")
                .importClass("com.wondernect.elements.rdb.response.PageResponseData")
                .importClass("com.wondernect.elements.easyoffice.excel.ESExcelItem")
                .importClass("javax.servlet.http.HttpServletRequest")
                .importClass("javax.servlet.http.HttpServletResponse")
                .addTo(serviceInterfaceDirectory);
        entityClass.setServiceInterfaceName(serviceInterfaceName);
    }

    /**
     * 创建服务抽象类
     */
    private void createServiceAbstract(EntityClass entityClass) {
        String dir = currentDirectory == null ? "service" : "service/" + currentDirectory;
        PsiDirectory serviceAbstractDirectory = directoryMap.get(dir);
        String serviceAbstractName = entityClass.getEntityName().concat("AbstractService");
        String excelExportName = entityClass.getEntityDescription() + "信息导出";
        String content = getCommentContent(entityClass.getEntityDescription() + "服务抽象实现类", entityClass.getAuthor()) +
                "\n@Service\npublic abstract class " + serviceAbstractName + " extends BaseService<" + entityClass.getResponseDTOName() + ", " + entityClass.getEntityName() + ", " + entityClass.getEntityIdType() + "> implements " + entityClass.getServiceInterfaceName() + "{\n" +

                "\n@Transactional" +
                "\n@Override" +
                "\npublic " + entityClass.getResponseDTOName() + " create(" + entityClass.getRequestDTOName() + " " + entityClass.getRequestDTOVariableName() + ") {" +
                "\n//TODO:判断对象是否存在" +
                "\n" +
                "\n" + entityClass.getEntityName() + " " + entityClass.getEntityVariableName() + " = new " + entityClass.getEntityName() + "();" +
                "\nESBeanUtils.copyProperties(" + entityClass.getRequestDTOVariableName() + ", " + entityClass.getEntityVariableName() + ");" +
                "\nreturn super.save(" + entityClass.getEntityVariableName() + ");" +
                "\n}" +

                "\n@Transactional" +
                "\n@Override" +
                "\npublic " + entityClass.getResponseDTOName() + " update(" + entityClass.getEntityIdType() + " " + entityClass.getEntityIdName() + ", " + entityClass.getRequestDTOName() + " " + entityClass.getRequestDTOVariableName() + ") {" +
                "\n" + entityClass.getEntityName() + " " + entityClass.getEntityVariableName() + " = super.findEntityById(" + entityClass.getEntityIdName() + ");" +
                "\nif (ESObjectUtils.isNull(" + entityClass.getEntityVariableName() + ")) {" +
                "\nthrow new BusinessException(\"" + entityClass.getEntityDescription() + "不存在\");" +
                "\n}" +
                "\nESBeanUtils.copyWithoutNullAndIgnoreProperties(" + entityClass.getRequestDTOVariableName() + ", " + entityClass.getEntityVariableName() + ");" +
                "\nreturn super.save(" + entityClass.getEntityVariableName() + ");" +
                "\n}" +

                "\n@Override" +
                "\npublic List<" + entityClass.getResponseDTOName() + "> list(" + entityClass.getListRequestDTOName() + " " + entityClass.getListRequestDTOVariableName() + ") {" +
                "\nCriteria<" + entityClass.getEntityName() + "> " + entityClass.getEntityVariableName() + "Criteria = new Criteria<>();" +
                "\n//TODO:添加列表筛选条件" +
                "\n" +
                "\nreturn super.findAll(" + entityClass.getEntityVariableName() + "Criteria, " + entityClass.getListRequestDTOVariableName() + ".getSortDataList());" +
                "\n}" +

                "\n@Override" +
                "\npublic PageResponseData<" + entityClass.getResponseDTOName() + "> page(" + entityClass.getPageRequestDTOName() + " " + entityClass.getPageRequestDTOVariableName() + ") {" +
                "\nCriteria<" + entityClass.getEntityName() + "> " + entityClass.getEntityVariableName() + "Criteria = new Criteria<>();" +
                "\n//TODO:添加分页筛选条件" +
                "\n" +
                "\nreturn super.findAll(" + entityClass.getEntityVariableName() + "Criteria, " + entityClass.getPageRequestDTOVariableName() + ".getPageRequestData());" +
                "\n}" +

                "\n@Override" +
                "\npublic List<ESExcelItem> excelItemList() {" +
                "\nreturn ESExcelUtils.getAllEntityExcelItem(" + entityClass.getResponseDTOName() + ".class);" +
                "\n}" +

                "\n@Override" +
                "\npublic void excelDataExport(" + entityClass.getListRequestDTOName() + " " + entityClass.getListRequestDTOVariableName() + ", HttpServletRequest request, HttpServletResponse response) {" +
                "\nList<" + entityClass.getResponseDTOName() + "> " + entityClass.getResponseDTOVariableName() + "List" + " = list(" + entityClass.getListRequestDTOVariableName() + ");" +
                "\nList<ESExcelItem> excelItemList = ESExcelUtils.getAllEntityExcelItem(" + entityClass.getResponseDTOName() + ".class);" +
                "\nList<ExcelExportEntity> titleList = new ArrayList<>();" +
                "\nif (CollectionUtils.isNotEmpty(excelItemList)) {" +
                "\nfor (ESExcelItem excelItem : excelItemList) {" +
                "\nexcelItem = excelItemHandle(excelItem);" +
                "\nif (!excelItem.getHidden()) {" +
                "\ntitleList.add(ESExcelUtils.generateExcelExportEntity(excelItem.getTitle(), excelItem.getName(), excelItem.getOrderNum()));" +
                "\n}" +
                "\n}" +
                "\n}" +
                "\nList<Map<String, Object>> dataList = ESExcelUtils.getEntityDataList(" + entityClass.getResponseDTOVariableName() + "List, excelItemList);" +
                "\nEasyExcel.exportExcel(titleList, dataList, \"" + excelExportName + "\", \"" + excelExportName + "\", \"" + excelExportName + "\", request, response);" +
                "\n}" +

                "\n@Override" +
                "\npublic " + entityClass.getResponseDTOName() + " generate(" + entityClass.getEntityName() + " " + entityClass.getEntityVariableName() + ") {" +
                "\n" + entityClass.getResponseDTOName() + " " + entityClass.getResponseDTOVariableName() + " = new " + entityClass.getResponseDTOName() + "();" +
                "\nESBeanUtils.copyProperties(" + entityClass.getEntityVariableName() + ", " + entityClass.getResponseDTOVariableName() + ");" +
                "\nreturn " + entityClass.getResponseDTOVariableName() + ";" +
                "\n}" +

                "\n@Override" +
                "\npublic ESExcelItem excelItemHandle(ESExcelItem excelItem) {" +
                "\nreturn excelItem;" +
                "\n}" +

                "\n}"
                ;
        getBaseClass(
                "BaseService",
                baseStringServiceClass -> ClassCreator.of(module).init(
                        serviceAbstractName,
                        content
                )
                        .importClass(entityClass.getEntityName())
                        .importClass(entityClass.getRequestDTOName())
                        .importClass(entityClass.getResponseDTOName())
                        .importClass(entityClass.getListRequestDTOName())
                        .importClass(entityClass.getPageRequestDTOName())
                        .importClass("org.springframework.stereotype.Service")
                        .importClass("org.springframework.transaction.annotation.Transactional")
                        .importClass("java.util.Map")
                        .importClass("java.util.List")
                        .importClass("java.util.ArrayList")
                        .importClass(baseStringServiceClass)
                        .importClass("com.wondernect.elements.rdb.criteria.Criteria")
                        .importClass("com.wondernect.elements.rdb.response.PageResponseData")
                        .importClass("com.wondernect.elements.common.utils.ESBeanUtils")
                        .importClass("com.wondernect.elements.common.utils.ESObjectUtils")
                        .importClass("com.wondernect.elements.common.exception.BusinessException")
                        .importClass("com.wondernect.elements.easyoffice.excel.EasyExcel")
                        .importClass("com.wondernect.elements.easyoffice.excel.ESExcelItem")
                        .importClass("com.wondernect.elements.easyoffice.excel.ESExcelUtils")
                        .importClass("cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity")
                        .importClass("org.apache.commons.collections4.CollectionUtils")
                        .importClass("javax.servlet.http.HttpServletRequest")
                        .importClass("javax.servlet.http.HttpServletResponse")
                        .addTo(serviceAbstractDirectory)
        );
        entityClass.setServiceAbstractName(serviceAbstractName);
    }

    /**
     * 创建服务实现类
     */
    private void createService(EntityClass entityClass) {
        String dir = currentDirectory == null ? "service" : "service/" + currentDirectory;
        PsiDirectory serviceDirectory = directoryMap.get(dir);
        String serviceName = entityClass.getEntityName().concat("Service");
        String content = getCommentContent(entityClass.getEntityDescription() + "服务", entityClass.getAuthor()) +
                "\n@Service\npublic class " + serviceName + " extends " + entityClass.getServiceAbstractName() + "{\n" +
                "\n}"
                ;
        ClassCreator.of(module).init(serviceName, content)
                .importClass("org.springframework.stereotype.Service")
                .addTo(serviceDirectory);
        entityClass.setServiceName(serviceName);
    }

    /**
     * 创建接口
     */
    private void createController(EntityClass entityClass) {
        String dir = currentDirectory == null ? "controller" : "controller/" + currentDirectory;
        PsiDirectory controllerDirectory = directoryMap.get(dir);
        String controllerName = entityClass.getEntityName().concat("Controller");
        String prefix = "/" + entityClass.getApiVersion() + "/" + entityClass.getApiService() + "/" + PsiStringUtils.toUnderLineStr(entityClass.getEntityName());
        String content = getCommentContent(entityClass.getEntityDescription() + "接口", entityClass.getAuthor()) +
                "\n@RequestMapping(value = \"" + prefix + "\")" +
                "\n@RestController" +
                "\n@Validated" +
                "\n@Api(tags = \"" + entityClass.getEntityDescription() + "接口\")" +
                "\npublic class " + controllerName +  "{\n" +
                "\n@Autowired\nprivate " + entityClass.getServiceName() + " " + entityClass.getServiceVariableName() + ";\n" +

                "\n@ApiOperation(value = \"创建\", httpMethod = \"POST\")\n@PostMapping(value = \"/create\")" +
                "\npublic BusinessData<" + entityClass.getResponseDTOName() + "> create(" +
                "\n@ApiParam(required = true) @NotNull(message = \"请求参数不能为空\") @Validated @RequestBody(required = false) " + entityClass.getRequestDTOName() + " " + entityClass.getRequestDTOVariableName() +
                "\n) {" +
                "\nreturn new BusinessData<>(" + entityClass.getServiceVariableName() + ".create(" + entityClass.getRequestDTOVariableName() +"));" +
                "\n}" +

                "\n@ApiOperation(value = \"更新\", httpMethod = \"POST\")\n@PostMapping(value = \"/{" + PsiStringUtils.toUnderLineStr(entityClass.getEntityIdName()) + "}/update\")" +
                "\npublic BusinessData<" + entityClass.getResponseDTOName() + "> update(" +
                "\n@ApiParam(required = true) @NotBlank(message = \"对象id不能为空\") @PathVariable(value = \"" + PsiStringUtils.toUnderLineStr(entityClass.getEntityIdName()) + "\", required = false) " + entityClass.getEntityIdType() + " " + entityClass.getEntityIdName() + "," +
                "\n@ApiParam(required = true) @NotNull(message = \"请求参数不能为空\") @Validated @RequestBody(required = false) " + entityClass.getRequestDTOName() + " " + entityClass.getRequestDTOVariableName() +
                "\n) {" +
                "\nreturn new BusinessData<>(" + entityClass.getServiceVariableName() + ".update("+ entityClass.getEntityIdName() +", " + entityClass.getRequestDTOVariableName() +"));" +
                "\n}" +

                "\n@ApiOperation(value = \"删除\", httpMethod = \"POST\")\n@PostMapping(value = \"/{" + PsiStringUtils.toUnderLineStr(entityClass.getEntityIdName()) + "}/delete\")" +
                "\npublic BusinessData delete(" +
                "\n@ApiParam(required = true) @NotBlank(message = \"对象id不能为空\") @PathVariable(value = \"" + PsiStringUtils.toUnderLineStr(entityClass.getEntityIdName()) + "\", required = false) " + entityClass.getEntityIdType() + " " + entityClass.getEntityIdName() +
                "\n) {" +
                "\n" + entityClass.getServiceVariableName() + ".deleteById("+ entityClass.getEntityIdName() +");" +
                "\nreturn new BusinessData(BusinessError.SUCCESS);" +
                "\n}" +

                "\n@ApiOperation(value = \"获取详细信息\", httpMethod = \"GET\")\n@GetMapping(value = \"/{" + PsiStringUtils.toUnderLineStr(entityClass.getEntityIdName()) + "}/detail\")" +
                "\npublic BusinessData<" + entityClass.getResponseDTOName() + "> detail(" +
                "\n@ApiParam(required = true) @NotBlank(message = \"对象id不能为空\") @PathVariable(value = \"" + PsiStringUtils.toUnderLineStr(entityClass.getEntityIdName()) + "\", required = false) " + entityClass.getEntityIdType() + " " + entityClass.getEntityIdName() +
                "\n) {" +
                "\nreturn new BusinessData<>(" + entityClass.getServiceVariableName() + ".findById("+ entityClass.getEntityIdName() +"));" +
                "\n}" +

                "\n@ApiOperation(value = \"列表\", httpMethod = \"POST\")\n@PostMapping(value = \"/list\")" +
                "\npublic BusinessData<List<" + entityClass.getResponseDTOName() + ">> list(" +
                "\n@ApiParam(required = true) @NotNull(message = \"列表请求参数不能为空\") @Validated @RequestBody(required = false) " + entityClass.getListRequestDTOName() + " " + entityClass.getListRequestDTOVariableName() +
                "\n) {" +
                "\nreturn new BusinessData<>(" + entityClass.getServiceVariableName() + ".list(" + entityClass.getListRequestDTOVariableName() +"));" +
                "\n}" +

                "\n@ApiOperation(value = \"分页\", httpMethod = \"POST\")\n@PostMapping(value = \"/page\")" +
                "\npublic BusinessData<PageResponseData<" + entityClass.getResponseDTOName() + ">> page(" +
                "\n@ApiParam(required = true) @NotNull(message = \"分页请求参数不能为空\") @Validated @RequestBody(required = false) " + entityClass.getPageRequestDTOName() + " " + entityClass.getPageRequestDTOVariableName() +
                "\n) {" +
                "\nreturn new BusinessData<>(" + entityClass.getServiceVariableName() + ".page(" + entityClass.getPageRequestDTOVariableName() +"));" +
                "\n}" +

                "\n@ApiOperation(value = \"获取excel的所有可用列名、类型、描述、get方法、set方法\", httpMethod = \"GET\")\n@GetMapping(value = \"/excel_item_list\")" +
                "\npublic BusinessData<List<ESExcelItem>> excelItemList() {" +
                "\nreturn new BusinessData<>(" + entityClass.getServiceVariableName() + ".excelItemList());" +
                "\n}" +

                "\n@ApiOperation(value = \"excel导出\", httpMethod = \"POST\")\n@PostMapping(value = \"/excel_data_export\")" +
                "\npublic void excelDataExport(" +
                "\n@ApiParam(required = true) @NotNull(message = \"列表请求参数不能为空\") @Validated @RequestBody(required = false) " + entityClass.getListRequestDTOName() + " " + entityClass.getListRequestDTOVariableName() + "," +
                "\nHttpServletRequest request," +
                "\nHttpServletResponse response" +
                "\n) {" +
                "\n" + entityClass.getServiceVariableName() + ".excelDataExport(" + entityClass.getListRequestDTOVariableName() +", request, response);" +
                "\n}" +

                "\n}"
                ;

        // 在controller目录下创建Controller
        ClassCreator.of(module)
                .init(controllerName, content)
                .importClass(entityClass.getServiceName())
                .importClass(entityClass.getRequestDTOName())
                .importClass(entityClass.getResponseDTOName())
                .importClass(entityClass.getListRequestDTOName())
                .importClass(entityClass.getPageRequestDTOName())
                .importClass("org.springframework.beans.factory.annotation.Autowired")
                .importClass("com.wondernect.elements.rdb.response.PageResponseData")
                .importClass("org.springframework.web.bind.annotation.RequestMaping")
                .importClass("org.springframework.web.bind.annotation.PostMapping")
                .importClass("org.springframework.web.bind.annotation.GetMapping")
                .importClass("io.swagger.annotations.Api")
                .importClass("io.swagger.annotations.ApiOperation")
                .importClass("io.swagger.annotations.ApiParam")
                .importClass("java.util.List")
                .importClass("org.springframework.web.bind.annotation.RequestMapping")
                .importClass("org.springframework.web.bind.annotation.RestController")
                .importClass("org.springframework.web.bind.annotation.RequestBody")
                .importClass("org.springframework.web.bind.annotation.PathVariable")
                .importClass("javax.validation.constraints.NotBlank")
                .importClass("javax.validation.constraints.NotNull")
                .importClass("com.wondernect.elements.common.response.BusinessData")
                .importClass("com.wondernect.elements.common.error.BusinessError")
                .importClass("org.springframework.validation.annotation.Validated")
                .importClass("com.wondernect.elements.easyoffice.excel.ESExcelItem")
                .importClass("com.wondernect.elements.easyoffice.excel.ESExcelUtils")
                .importClass("javax.servlet.http.HttpServletRequest")
                .importClass("javax.servlet.http.HttpServletResponse")
                .addTo(controllerDirectory);
    }

    /**
     * 获取base class
     */
    private void getBaseClass(String baseClassName, Consumer<PsiClass> consumer) {
        Optional<PsiClass> baseClassOptional = psiUtils.findClass(baseClassName);
        if (baseClassOptional.isPresent()) {
            PsiClass psiClass = baseClassOptional.get();
            consumer.accept(psiClass);
        }
    }

    private String getCommentContent(String desc, String author) {
        return "/** " + desc +
                " \n * @author " + author + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                " **/";
    }

    private boolean generateEntityIdField(EntityClass entityClass) {
        String idType = null;
        String idName = null;
        for (PsiField field : entityClass.getEntityClass().getFields()) {
            PsiAnnotation fieldAnnotation = field.getAnnotation("javax.persistence.Id");
            if (fieldAnnotation != null) {
                idName = field.getName();
                PsiType type = field.getType();
                String typeName = type.getCanonicalText();
                if (typeName.contains(".")) {
                    idType = typeName.substring(typeName.lastIndexOf(".") + 1);
                }
            }
        }
        if (idType == null || idName == null) {
            return false;
        }
        if (!"String".equals(idType) && !"Long".equals(idType)) {
            return false;
        }
        entityClass.setEntityIdType(idType);
        entityClass.setEntityIdName(idName);
        return true;
    }

    private String getFieldsContentForREQDTO(EntityClass entityClass) {
        String content = "";
        for (PsiField field : entityClass.getEntityClass().getFields()) {
            String name = field.getName();
            PsiType type = field.getType();
            String typeName = type.getCanonicalText();
            if (typeName.contains(".")) {
                typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
            }
            // @Id注解
            PsiAnnotation idAnnotation = field.getAnnotation("javax.persistence.Id");
            if (idAnnotation != null) {
                continue;
            }
            // @ApiModelProperty注解
            PsiAnnotation fieldAnnotation = field.getAnnotation("io.swagger.annotations.ApiModelProperty");
            Optional<String> descriptionOptional = psiUtils.getAnnotationValue(fieldAnnotation, "value");
            String description = null;
            if (descriptionOptional.isPresent()) {
                description = descriptionOptional.get();
            } else {
                descriptionOptional = psiUtils.getAnnotationValue(fieldAnnotation, "notes");
                if (descriptionOptional.isPresent()) {
                    description = descriptionOptional.get();
                }
            }
            assert description != null;
            description = description.replace("\"", "");
            // @Column注解
            PsiAnnotation psiAnnotation = field.getAnnotation("javax.persistence.Column");
            if (null != psiAnnotation) {
                PsiAnnotationMemberValue memberValue = psiAnnotation.findAttributeValue("columnDefinition");
                if (null != memberValue) {
                    String str = memberValue.getText();
                    if (typeName.equals("String")) {
                        // 只有字符串的时候才添加长度限制
                        if (str.contains("varchar") || str.contains("char")) {
                            str = str.replace("varchar(", "").replace("char(", "");
                            int idx = str.indexOf(")");
                            if (-1 != idx) {
                                String lengthStr = str.substring(0, idx).replaceAll("\"", "");
                                if (StringUtils.isNotBlank(lengthStr)) {
                                    int length = Integer.parseInt(lengthStr);
                                    content = content + "\n@Length(max = " + length + ", message = \"" + description + "长度不能超过" + length + "字符(1汉字=2字符)\")";
                                }
                            }
                        }
                    }
                    // 如果是not null，需要加上NotNull校验 javax.validation.constraints
                    if (str.contains("not null") && !typeName.toLowerCase().contains("type")) {
                        if (typeName.equals("String")) {
                            content = content + "\n@NotBlank(message = \"" + description + "不能为空\")";
                        } else {
                            content = content + "\n@NotNull(message = \"" + description + "不能为空\")";
                        }
                    }
                }
            }
            if (typeName.toLowerCase().equals("localdate")) {
                content = content + "\n@JsonFormat(pattern = \"yyyy-MM-dd\")";
            } else if (typeName.toLowerCase().equals("localdatetime")) {
                content = content + "\n@JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")";
            }
            content = content + "\n@JsonProperty(\"" + PsiStringUtils.toUnderLineStr(name) + "\")";
            content = content + "\n@ApiModelProperty(notes = \"" + description + "\")";
            content = content + "\nprivate " + typeName + " " + name + ";\n";
        }
        return content;
    }

    private String getFieldsContentForRESDTO(EntityClass entityClass) {
        String content = "";
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
            if (descriptionOptional.isPresent()) {
                description = descriptionOptional.get();
            } else {
                descriptionOptional = psiUtils.getAnnotationValue(fieldAnnotation, "notes");
                if (descriptionOptional.isPresent()) {
                    description = descriptionOptional.get();
                }
            }
            assert description != null;
            description = description.replace("\"", "");

            if (typeName.toLowerCase().equals("localdate")) {
                content = content + "\n@JsonFormat(pattern = \"yyyy-MM-dd\")";
            } else if (typeName.toLowerCase().equals("localdatetime")) {
                content = content + "\n@JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")";
            }
            content = content + "\n@JsonProperty(\"" + PsiStringUtils.toUnderLineStr(name) + "\")";
            content = content + "\n@ApiModelProperty(notes = \"" + description + "\")";
            content = content + "\nprivate " + typeName + " " + name + ";\n";
        }
        return content;
    }
}
