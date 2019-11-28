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

import com.sun.javadoc.ClassDoc;
import io.swagger.annotations.Api;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;

import static java.util.Objects.nonNull;

/**
 * @author uhfun
 */
public class ApiInfo {
    public static final String HIDDEN = "hidden";
    public static final String TAGS = "tags";
    private String tag;
    private boolean hidden;
    private boolean deprecated;
    private Class clazz;
    private ClassDoc classDoc;

    private ApiInfo(Class clazz, ClassDoc classDoc) {
        this.clazz = clazz;
        this.classDoc = classDoc;
        tag = clazz.getSimpleName();
    }

    public static ApiInfo fromClassDoc(Class clazz, ClassDoc classDoc) {
        ApiInfo apiInfo = new ApiInfo(clazz, classDoc);
        apiInfo.readClassDoc();
        apiInfo.readApi();
        return apiInfo;
    }

    private void readClassDoc() {
        hidden |= classDoc.getRawCommentText().contains("@hidden");
        deprecated |= classDoc.getRawCommentText().contains("@deprecated");
        if (!StringUtils.isEmpty(classDoc.getRawCommentText())) {
            tag = classDoc.commentText();
        }
    }

    private void readApi() {
        Api api = findAnnotation(Api.class);
        if (nonNull(api)) {
            hidden |= api.hidden();
            for (String tag : api.tags()) {
                if (!StringUtils.isEmpty(tag)) {
                    this.tag = tag;
                }
            }
        }
    }

    public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(clazz, annotationType);
    }

    public boolean hidden() {
        return hidden;
    }

    public boolean deprecated() {
        return deprecated;
    }

    public String tag() {
        return tag;
    }
}
