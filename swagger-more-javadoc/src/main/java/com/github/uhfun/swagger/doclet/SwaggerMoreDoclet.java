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
package com.github.uhfun.swagger.doclet;

import com.github.uhfun.swagger.ApiInfo;
import com.github.uhfun.swagger.ApiMethodInfo;
import com.github.uhfun.swagger.DocLogger;
import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.common.SwaggerMoreException;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static com.github.uhfun.swagger.ApiInfo.TAGS;
import static com.github.uhfun.swagger.ApiMethodInfo.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

/**
 * @author uhfun
 */
public class SwaggerMoreDoclet {
    public static final String OPTION_CLASS_DIR = "-classDir";
    private static String classDir;

    public static
    void main(String[] args) {
        // 调试入口
        String[] docArgs = new String[]{"-doclet", SwaggerMoreDoclet.class.getName(),
                System.getProperty("user.dir") +
                        "/swagger-more-annotations/src/main/java/com/github/uhfun/swagger/annotations/demoApi/AuthorityService.java",
                "-classDir", System.getProperty("user.dir") +
                "/swagger-more-annotations/target/classes"};
        com.sun.tools.javadoc.Main.execute(docArgs);
    }

    public static boolean start(RootDoc rootDoc) {
        // 插件启动入口
        DocLogger.setRootDoc(rootDoc);
        if (StringUtils.isEmpty(classDir)) {
            DocLogger.error("-classDir is not specified.");
            return false;
        }
        DocLogger.info("Writing classes to " + classDir);
        try {
            parseAndAnnotate(rootDoc);
        } catch (Exception e) {
            DocLogger.error(e.getMessage() + "\n" +
                    Stream.of(e.getStackTrace()).map(StackTraceElement::toString)
                            .collect(joining("\n")));
            if (nonNull(e.getCause())) {
                DocLogger.error(e.getCause().getMessage() + "\n" +
                        Stream.of(e.getStackTrace()).map(StackTraceElement::toString)
                                .collect(joining("\n")));
            }
        }
        return true;
    }

    private static void parseAndAnnotate(RootDoc rootDoc) throws ClassNotFoundException, NotFoundException, CannotCompileException, IOException {
        for (ClassDoc classDoc : rootDoc.classes()) {
            if (StringUtils.isEmpty(classDoc.getRawCommentText())) {
                DocLogger.warn("No javadoc found in class " + classDoc.qualifiedName() + "." + classDoc.name());
            }
            Class clazz = Class.forName(classDoc.qualifiedName());
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new ClassClassPath(clazz));
            CtClass ctClass = pool.get(clazz.getName());
            ApiInfo apiInfo = ApiInfo.fromClassDoc(clazz, classDoc);
            if (!apiInfo.hidden()) {
                annotateClassAnn(ctClass, apiInfo);
                for (MethodDoc methodDoc : classDoc.methods()) {
                    ApiMethodInfo apiMethodInfo = ApiMethodInfo.fromMethodDoc(matchingMethod(clazz, methodDoc), methodDoc);
                    if (StringUtils.isEmpty(methodDoc.getRawCommentText())) {
                        DocLogger.warn("No javadoc found in method " + classDoc.qualifiedName() + "." + methodDoc.name() + methodDoc.signature());
                    }
                    annotateMethodAnn(ctClass, apiMethodInfo);
                }
                DocLogger.info("Successfully annotated " + clazz.getTypeName());
            }
            ctClass.writeFile(classDir);
        }
    }

    private static void annotateClassAnn(CtClass ctClass, ApiInfo apiInfo) {
        ConstPool constPool = ctClass.getClassFile().getConstPool();
        AnnotationsAttribute attr = getAnnotationAttr(ctClass);
        annotateDeprecatedAnn(apiInfo, attr, constPool);
        annotateApiAnn(apiInfo, attr, constPool);
        ctClass.getClassFile().addAttribute(attr);
    }

    private static void annotateDeprecatedAnn(ApiInfo info, AnnotationsAttribute attr, ConstPool constPool) {
        if (info.deprecated() && isNull(attr.getAnnotation(Deprecated.class.getTypeName()))) {
            attr.addAnnotation(new Annotation(Deprecated.class.getName(), constPool));
        }
    }

    private static void annotateApiAnn(ApiInfo info, AnnotationsAttribute attr, ConstPool constPool) {
        Annotation apiAnn = attr.getAnnotation(Api.class.getTypeName());
        MemberValue value;
        if (isNull(apiAnn)) {
            apiAnn = new Annotation(Api.class.getName(), constPool);
        }
        if (isNull(value = apiAnn.getMemberValue(ApiInfo.HIDDEN)) || !((BooleanMemberValue) value).getValue()) {
            apiAnn.addMemberValue(ApiInfo.HIDDEN, new BooleanMemberValue(info.hidden(), constPool));
        }
        ArrayMemberValue arrayMemberValue = (ArrayMemberValue) apiAnn.getMemberValue(TAGS);
        if (isNull(arrayMemberValue)) {
            arrayMemberValue = new ArrayMemberValue(constPool);
            arrayMemberValue.setValue(new MemberValue[1]);
        }
        StringMemberValue tagMemberValue = (StringMemberValue) arrayMemberValue.getValue()[0];
        if (isNull(tagMemberValue) || StringUtils.isEmpty(tagMemberValue.getValue())) {
            tagMemberValue = new StringMemberValue(info.tag(), constPool);
        }
        tagMemberValue.setValue(info.tag());
        arrayMemberValue.getValue()[0] = tagMemberValue;
        apiAnn.addMemberValue(TAGS, arrayMemberValue);
        attr.addAnnotation(apiAnn);
    }

    private static void annotateMethodAnn(CtClass ctClass,
                                          ApiMethodInfo methodInfo) throws NotFoundException {
        ConstPool constPool = ctClass.getClassFile().getConstPool();
        for (CtMethod ctMethod : ctClass.getDeclaredMethods(methodInfo.methodName())) {
            if (Stream.of(ctMethod.getParameterTypes()).map(CtClass::getSimpleName).collect(joining(", ")).equals(methodInfo.parameterNames())) {
                AnnotationsAttribute attr = getAnnotationAttr(ctMethod);
                annotateDeprecatedAnn(methodInfo, attr, constPool);
                annotateApiMethodAnn(methodInfo, attr, constPool);
                ctMethod.getMethodInfo().addAttribute(attr);
            }
        }
    }

    private static void annotateDeprecatedAnn(ApiMethodInfo methodInfo, AnnotationsAttribute attr, ConstPool constPool) {
        if (methodInfo.deprecated() && isNull(attr.getAnnotation(Deprecated.class.getTypeName()))) {
            attr.addAnnotation(new Annotation(Deprecated.class.getName(), constPool));
        }
    }

    private static void annotateApiMethodAnn(ApiMethodInfo methodInfo, AnnotationsAttribute attr, ConstPool constPool) {
        Annotation apiMethodAnn = attr.getAnnotation(ApiMethod.class.getTypeName());
        MemberValue value;
        if (isNull(apiMethodAnn)) {
            apiMethodAnn = new Annotation(ApiMethod.class.getName(), constPool);
        }
        if (isNull(value = apiMethodAnn.getMemberValue(ApiMethodInfo.VALUE)) || StringUtils.isEmpty(((StringMemberValue) value).getValue())) {
            apiMethodAnn.addMemberValue(ApiMethodInfo.VALUE, new StringMemberValue(methodInfo.value(), constPool));
        }
        if (isNull(value = apiMethodAnn.getMemberValue(HIDDEN)) || !((BooleanMemberValue) value).getValue()) {
            apiMethodAnn.addMemberValue(HIDDEN, new BooleanMemberValue(methodInfo.hidden(), constPool));
        }
        if (isNull(value = apiMethodAnn.getMemberValue(NOTES)) || StringUtils.isEmpty(((StringMemberValue) value).getValue())) {
            apiMethodAnn.addMemberValue(NOTES, new StringMemberValue(methodInfo.notes(), constPool));
        }
        ArrayMemberValue arrayMemberValue = (ArrayMemberValue) apiMethodAnn.getMemberValue("params");
        if (isNull(arrayMemberValue)) {
            arrayMemberValue = new ArrayMemberValue(constPool);
            arrayMemberValue.setValue(new MemberValue[methodInfo.parameterCount()]);
        }
        AnnotationMemberValue annotationMemberValue;
        for (int i = 0; i < methodInfo.parameterCount(); i++) {
            if (isNull(annotationMemberValue = (AnnotationMemberValue) arrayMemberValue.getValue()[i])) {
                annotationMemberValue = new AnnotationMemberValue(new Annotation(ApiParam.class.getName(), constPool), constPool);
            }
            Annotation apiParamAnn = annotationMemberValue.getValue();
            if (isNull(value = apiParamAnn.getMemberValue(NAME)) || StringUtils.isEmpty(((StringMemberValue) value).getValue())) {
                apiParamAnn.addMemberValue(NAME, new StringMemberValue(methodInfo.param(i).name(), constPool));
            }
            if (isNull(value = apiParamAnn.getMemberValue(ApiMethodInfo.VALUE)) || StringUtils.isEmpty(((StringMemberValue) value).getValue())) {
                apiParamAnn.addMemberValue(ApiMethodInfo.VALUE, new StringMemberValue(methodInfo.param(i).value(), constPool));
            }
            arrayMemberValue.getValue()[i] = annotationMemberValue;

        }
        apiMethodAnn.addMemberValue(PARAMS, arrayMemberValue);
        attr.addAnnotation(apiMethodAnn);
    }

    public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
        // 插件参数校验需要实现的方法
        Arrays.stream(options).forEach(s -> {
            if (OPTION_CLASS_DIR.equalsIgnoreCase(s[0])) {
                classDir = s[1];
            }
        });
        return true;
    }

    public static int optionLength(String option) {
        return OPTION_CLASS_DIR.equalsIgnoreCase(option) ? 2 : 0;
    }

    private static AnnotationsAttribute getAnnotationAttr(CtClass ctClass) {
        for (Object o : ctClass.getClassFile().getAttributes()) {
            if (o instanceof AnnotationsAttribute) {
                return (AnnotationsAttribute) o;
            }
        }
        return new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
    }

    private static AnnotationsAttribute getAnnotationAttr(CtMethod ctMethod) {
        for (Object o : ctMethod.getMethodInfo().getAttributes()) {
            if (o instanceof AnnotationsAttribute) {
                return (AnnotationsAttribute) o;
            }
        }
        return new AnnotationsAttribute(ctMethod.getMethodInfo().getConstPool(), AnnotationsAttribute.visibleTag);
    }

    private static Method matchingMethod(Class clazz, MethodDoc methodDoc) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodDoc.name())) {
                continue;
            }
            String methodSignature = "(" + Stream.of(method.getParameterTypes()).map(Class::getName).collect(joining(", ")) + ")";
            if (methodSignature.equals(methodDoc.signature())) {
                return method;
            }
        }
        throw new SwaggerMoreException("Unable to find the corresponding method based on methodDoc " +
                methodDoc.name() + methodDoc.signature());
    }
}
