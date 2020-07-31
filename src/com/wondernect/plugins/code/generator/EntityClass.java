package com.wondernect.plugins.code.generator;

import com.intellij.psi.PsiClass;
import com.wondernect.plugins.code.generator.util.PsiStringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局配置实体类
 */
public class EntityClass {

    private String author;
    private String apiVersion;
    private String apiService;

    private PsiClass entityClass;
    private String entityName;
    private String entityVariableName;
    private String entityDescription;

    private String entityIdType;
    private String entityIdName;
    private String entityIdVariableName;

    private String repositoryName;
    private String repositoryVariableName;

    private String daoName;
    private String daoVariableName;

    private String managerName;
    private String managerVariableName;

    private String requestDTOName;
    private String requestDTOVariableName;

    private String responseDTOName;
    private String responseDTOVariableName;
    private Map<String, String> responseFields = new HashMap<>();
    private Map<String, String> responseFieldsDescription = new HashMap<>();

    private String listRequestDTOName;
    private String listRequestDTOVariableName;

    private String pageRequestDTOName;
    private String pageRequestDTOVariableName;

    private String serviceInterfaceName;
    private String serviceInterfaceVariableName;

    private String serviceAbstractName;
    private String serviceAbstractVariableName;

    private String serviceName;
    private String serviceVariableName;

    private String controllerName;
    private String controllerVariableName;

    public EntityClass(String author, String apiVersion, String apiService) {
        this.author = author;
        this.apiVersion = apiVersion;
        this.apiService = apiService;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApiService() {
        return apiService;
    }

    public void setApiService(String apiService) {
        this.apiService = apiService;
    }

    EntityClass setEntityClass(PsiClass entityClass) {
        this.entityClass = entityClass;
        this.entityName = entityClass.getName();
        this.entityVariableName = PsiStringUtils.firstLetterToLower(this.entityName);
        return this;
    }

    PsiClass getEntityClass() {
        return this.entityClass;
    }

    String getEntityName() {
        return this.entityName;
    }

    String getEntityVariableName() {
        return this.entityVariableName;
    }

    public String getEntityDescription() {
        return entityDescription;
    }

    public void setEntityDescription(String entityDescription) {
        this.entityDescription = entityDescription;
    }

    public String getEntityIdType() {
        return entityIdType;
    }

    public void setEntityIdType(String entityIdType) {
        this.entityIdType = entityIdType;
    }

    public String getEntityIdName() {
        return entityIdName;
    }

    public void setEntityIdName(String entityIdName) {
        this.entityIdName = entityIdName;
        this.entityIdVariableName = PsiStringUtils.firstLetterToLower(entityIdName);
    }

    public String getEntityIdVariableName() {
        return entityIdVariableName;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        this.repositoryVariableName = PsiStringUtils.firstLetterToLower(repositoryName);
    }

    public String getRepositoryVariableName() {
        return repositoryVariableName;
    }

    public String getDaoName() {
        return daoName;
    }

    public void setDaoName(String daoName) {
        this.daoName = daoName;
        this.daoVariableName = PsiStringUtils.firstLetterToLower(daoName);
    }

    public String getDaoVariableName() {
        return daoVariableName;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
        this.managerVariableName = PsiStringUtils.firstLetterToLower(managerName);
    }

    public String getManagerVariableName() {
        return managerVariableName;
    }

    public String getRequestDTOName() {
        return requestDTOName;
    }

    public void setRequestDTOName(String requestDTOName) {
        this.requestDTOName = requestDTOName;
        this.requestDTOVariableName = PsiStringUtils.firstLetterToLower(requestDTOName);
    }

    public String getRequestDTOVariableName() {
        return requestDTOVariableName;
    }

    public String getResponseDTOName() {
        return responseDTOName;
    }

    public void setResponseDTOName(String responseDTOName) {
        this.responseDTOName = responseDTOName;
        this.responseDTOVariableName = PsiStringUtils.firstLetterToLower(responseDTOName);
    }

    public String getResponseDTOVariableName() {
        return responseDTOVariableName;
    }

    public Map<String, String> getResponseFields() {
        return responseFields;
    }

    public void setResponseFields(Map<String, String> responseFields) {
        this.responseFields = responseFields;
    }

    public Map<String, String> getResponseFieldsDescription() {
        return responseFieldsDescription;
    }

    public void setResponseFieldsDescription(Map<String, String> responseFieldsDescription) {
        this.responseFieldsDescription = responseFieldsDescription;
    }

    public String getListRequestDTOName() {
        return listRequestDTOName;
    }

    public void setListRequestDTOName(String listRequestDTOName) {
        this.listRequestDTOName = listRequestDTOName;
        this.listRequestDTOVariableName = PsiStringUtils.firstLetterToLower(listRequestDTOName);
    }

    public String getListRequestDTOVariableName() {
        return listRequestDTOVariableName;
    }

    public String getPageRequestDTOName() {
        return pageRequestDTOName;
    }

    public void setPageRequestDTOName(String pageRequestDTOName) {
        this.pageRequestDTOName = pageRequestDTOName;
        this.pageRequestDTOVariableName = PsiStringUtils.firstLetterToLower(pageRequestDTOName);
    }

    public String getPageRequestDTOVariableName() {
        return pageRequestDTOVariableName;
    }

    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    public void setServiceInterfaceName(String serviceInterfaceName) {
        this.serviceInterfaceName = serviceInterfaceName;
        this.serviceInterfaceVariableName = PsiStringUtils.firstLetterToLower(serviceInterfaceName);
    }

    public String getServiceInterfaceVariableName() {
        return serviceInterfaceVariableName;
    }

    public String getServiceAbstractName() {
        return serviceAbstractName;
    }

    public void setServiceAbstractName(String serviceAbstractName) {
        this.serviceAbstractName = serviceAbstractName;
        this.serviceAbstractVariableName = PsiStringUtils.firstLetterToLower(serviceAbstractName);
    }

    public String getServiceAbstractVariableName() {
        return serviceAbstractVariableName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        this.serviceVariableName = PsiStringUtils.firstLetterToLower(serviceName);
    }

    public String getServiceVariableName() {
        return serviceVariableName;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
        this.controllerVariableName = PsiStringUtils.firstLetterToLower(controllerName);
    }

    public String getControllerVariableName() {
        return controllerVariableName;
    }
}
