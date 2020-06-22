package com.wondernect.plugins.code.generator;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.wondernect.plugins.code.generator.util.PsiUtils;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 类创建器
 **/
public class ClassCreator {
    private PsiJavaFile javaFile;
    private Project project;
    private PsiUtils psiUtils;

    private ClassCreator(Module module) {
        this.psiUtils = PsiUtils.of(module);
        this.project = module.getProject();
    }

    static ClassCreator of(Module module) {
        return new ClassCreator(module);
    }

    ClassCreator init(String name, String content) {
        javaFile = (PsiJavaFile) PsiFileFactory.getInstance(project).createFileFromText(name + ".java", JavaFileType.INSTANCE, content);
        return this;
    }

    ClassCreator importClass(String className) {
        if (org.apache.commons.lang3.StringUtils.isBlank(className)) {
            return this;
        }
        psiUtils.findClass(className).ifPresent(javaFile::importClass);
        return this;
    }

    ClassCreator importClass(PsiClass psiClass) {
        if (null == psiClass) {
            return this;
        }
        javaFile.importClass(psiClass);
        return this;
    }

    And addTo(PsiDirectory psiDirectory) {
        return new And(((PsiJavaFile)Optional.ofNullable(psiDirectory.findFile(javaFile.getName())).orElseGet(() -> {
            psiUtils.format(javaFile);
            return (PsiJavaFile)psiDirectory.add(javaFile);
        })).getClasses()[0]);
    }

    ClassCreator addGetterAndSetterMethods() {
        PsiClass aClass = javaFile.getClasses()[0];
        psiUtils.addGetterAndSetterMethods(aClass);
        return this;
    }

    public static class And {
        private PsiClass psiClass;

        private And(PsiClass psiClass) {
            this.psiClass = psiClass;
        }

        void and(Consumer<PsiClass> callback) {
            callback.accept(psiClass);
        }
    }
}
