/*
 *
 *  Copyright 2019 uhfun
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package com.github.uhfun.swagger;

import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.common.SwaggerMoreException;
import com.google.common.collect.Lists;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import io.swagger.annotations.ApiParam;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

/**
 * @author uhfun
 */
public class ApiMethodInfo {
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String NOTES = "notes";
    public static final String PARAMS = "params";
    public static final String HIDDEN = "hidden";
    private String value;
    private StringBuilder noteBuilder;
    private String returnDescription;
    private boolean hidden;
    private boolean deprecated;
    private Method method;
    private List<ApiParamInfo> paramInfos = Lists.newArrayList();
    private MethodDoc methodDoc;

    private ApiMethodInfo(Method method, MethodDoc methodDoc) {
        this.method = method;
        this.methodDoc = methodDoc;
        value = "";
        returnDescription = method.getReturnType().getSimpleName();
        noteBuilder = new StringBuilder();
    }

    public static ApiMethodInfo fromMethodDoc(Method method, MethodDoc methodDoc) throws SwaggerMoreException {
        ApiMethodInfo methodInfo = new ApiMethodInfo(method, methodDoc);
        methodInfo.readMethodDoc();
        methodInfo.readApiMethod();
        return methodInfo;
    }

    private void readMethodDoc() {
        hidden |= methodDoc.getRawCommentText().contains("@hidden");
        deprecated |= methodDoc.getRawCommentText().contains("@deprecated");
        if (methodDoc.tags("see").length > 0) {
            noteBuilder.append("<b> 查看 -> " + methodDoc.tags("see")[0].text() + " </b>\n");
        }
        if (!StringUtils.isEmpty(methodDoc.commentText())) {
            value = methodDoc.commentText();
        }
        if (methodDoc.tags("return").length > 0) {
            returnDescription = methodDoc.tags("return")[0].text();
        }
        for (ParamTag paramTag : methodDoc.paramTags()) {
            paramInfos.add(ApiParamInfo.fromParamTag(paramTag));
        }
        if (methodDoc.paramTags().length != parameterCount()) {
            DocLogger.warn("The number of comment parameters for method [" +
                    method.getDeclaringClass().getName() + "." + method.getName() +
                    "(" + parameterNames() + ")] in javadoc is incorrect");
        }
        if (paramInfos.size() != parameterCount()) {
            for (int i = paramInfos.size(); i < parameterCount(); i++) {
                String defaultName = methodDoc.parameters()[i].name();
                paramInfos.add(ApiParamInfo.defaultInfo(defaultName));
            }
        }
    }

    private void readApiMethod() {
        ApiMethod apiMethod = findAnnotation(ApiMethod.class);
        if (nonNull(apiMethod)) {
            hidden |= apiMethod.hidden();
            if (!StringUtils.isEmpty(apiMethod.value())) {
                value = apiMethod.value();
            }
            noteBuilder.append(apiMethod.notes()).append("\n");
            if (!StringUtils.isEmpty(apiMethod.returnDescription())) {
                returnDescription = apiMethod.returnDescription();
            }
            if (apiMethod.params().length != parameterCount()) {
                DocLogger.warn("The number of @ApiParams for method [" +
                        method.getDeclaringClass().getName() + "." + method.getName() +
                        "(" + parameterNames() + ")] in @ApiMethod is incorrect");
            }
            ApiParam param;
            for (int i = 0; i < parameterCount(); i++) {
                param = apiMethod.params()[i];
                if (!StringUtils.isEmpty(param.name())) {
                    paramInfos.get(i).setName(param.name());
                }
                if (!StringUtils.isEmpty(param.value())) {
                    paramInfos.get(i).setValue(param.value());
                }
            }
        }
    }

    public String notes() {
        return noteBuilder.toString();
    }

    public String methodName() {
        return method.getName();
    }

    public String parameterNames() {
        return Stream.of(method.getParameterTypes()).map(Class::getSimpleName).collect(joining(", "));
    }

    public int parameterCount() {
        return method.getParameterCount();
    }

    public String value() {
        return value;
    }

    public boolean hidden() {
        return hidden;
    }

    public boolean deprecated() {
        return deprecated;
    }

    public ApiParamInfo param(int index) {
        if (index > paramInfos.size() - 1) {
            return ApiParamInfo.defaultInfo(index);
        }
        return paramInfos.get(index);
    }

    public List<ApiParamInfo> paramInfos() {
        return paramInfos;
    }

    public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(method, annotationType);
    }

    public String returnDescription() {
        return returnDescription;
    }
}
