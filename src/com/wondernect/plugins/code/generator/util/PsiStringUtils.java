package com.wondernect.plugins.code.generator.util;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Objects;

public final class PsiStringUtils {

    public static String firstLetterToUpper(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return str.replaceFirst(str.substring(0, 1), str.substring(0, 1).toUpperCase());
    }

    public static String firstLetterToLower(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return str.replaceFirst(str.substring(0, 1), str.substring(0, 1).toLowerCase());
    }

    public static String toUnderLineStr(String str) {
        return firstLetterToLower(Arrays.stream(Objects.requireNonNull(org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase(str)))
                .reduce((s1, s2) -> s1.toLowerCase().concat("_").concat(s2.toLowerCase())).orElse(""));
    }
}
