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
package com.github.uhfun.swagger.util;

import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.webmvc.DubboHandlerMethod;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;

import static com.github.uhfun.swagger.common.Constant.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

/**
 * @author uhfun
 */
@Slf4j
public class ProxyUtils {

    public static Method createMethodProxy(Method method, DubboHandlerMethod.HandlerMethodProxy handlerMethodProxy) {
        Class clazz = mergeParametersIntoClass(method);
        Method methodProxy;
        if (isNull(clazz) || isNull(methodProxy = ReflectionUtils.findMethod(clazz, FORWARD_INVOCATION, clazz))) {
            log.warn("Create method proxy failed.");
            return method;
        }
        try {
            ReflectionUtils.findMethod(clazz, HANDLER_METHOD_PROXY_SETTER, DubboHandlerMethod.HandlerMethodProxy.class)
                    .invoke(null, handlerMethodProxy);
        } catch (Exception e) {
            log.warn("Set handlerMethodProxy failed.", e);
            return method;
        }
        return methodProxy;
    }

    private static Class mergeParametersIntoClass(Method method) {
        Parameter[] parameters = method.getParameters();
        String generatedClassName = generateName(method);
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass;
        try {
            ctClass = pool.get(generatedClassName);
        } catch (NotFoundException notFoundException) {
            ctClass = pool.makeClass(generatedClassName);
            try {
                ApiMethod apiMethod = AnnotationUtils.findAnnotation(method, ApiMethod.class);
                ApiParam apiParam = null;
                for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
                    Parameter parameter = parameters[i];
                    if (nonNull(apiMethod) && apiMethod.params().length >= i) {
                        apiParam = apiMethod.params()[i];
                    }
                    ctClass.addField(createField(parameter, apiParam, ctClass));
                }
                CtField actualMethodField = new CtField(ClassPool.getDefault().get(DubboHandlerMethod.HandlerMethodProxy.class.getName()), "proxy", ctClass);
                actualMethodField.setModifiers(Modifier.STATIC);
                ctClass.addField(actualMethodField);
                ctClass.addMethod(createForwardInvocationMethod(apiMethod, ctClass));
                ctClass.addMethod(createInitMethod(ctClass));
                ctClass.writeFile("target/classes");
                return ctClass.toClass();
            } catch (Exception e) {
                log.error("Dynamically generated class error :", e);
            }
        }
        if (nonNull(ctClass)) {
            try {
                return pool.getClassLoader().loadClass(generatedClassName);
            } catch (ClassNotFoundException e) {
                log.error("ClassNotFoundException : ", e);
            }
        }
        return null;
    }

    private static String generateName(Method method) {
        return DEFAULT_PACKAGE_NAME +
                method.getDeclaringClass().getSimpleName() + DOT +
                GENERATED_PREFIX + method.getName() + UNDERLINE +
                Stream.of(method.getParameterTypes()).map(Class::getSimpleName).collect(joining("_")) +
                DEFAULT_COMPLEX_OBJECT_SUFFIX;
    }

    private static CtField createField(Parameter parameter, ApiParam apiParam, CtClass ctClass) throws NotFoundException, CannotCompileException {
        Class fieldType = parameter.getType();
        String fieldName = nonNull(apiParam) ? apiParam.name() : parameter.getName();
        String fieldValue = nonNull(apiParam) ? apiParam.value() : parameter.getName();
        ClassPool.getDefault().insertClassPath(new ClassClassPath(fieldType));
        CtField field = new CtField(ClassPool.getDefault().get(fieldType.getName()), fieldName, ctClass);
        field.setModifiers(Modifier.PUBLIC);
        ConstPool constPool = ctClass.getClassFile().getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation ann = new Annotation(ApiModelProperty.class.getName(), constPool);
        ann.addMemberValue("value", new StringMemberValue(fieldValue, constPool));
        ann.addMemberValue("name", new StringMemberValue(fieldName, constPool));
        ann.addMemberValue("required", new BooleanMemberValue(true, constPool));
        attr.addAnnotation(ann);
        field.getFieldInfo().addAttribute(attr);
        return field;
    }

    private static CtMethod createInitMethod(CtClass ctClass) throws CannotCompileException {
        return CtMethod.make("public static void " + HANDLER_METHOD_PROXY_SETTER +
                " (com.github.uhfun.swagger.webmvc.DubboHandlerMethod.HandlerMethodProxy proxy){ " +
                ctClass.getName() + ".proxy = proxy;}", ctClass);
    }

    private static CtMethod createForwardInvocationMethod(ApiMethod apiMethod, CtClass declaring) throws CannotCompileException {
        String format = "public static Object %s (%s param0) throws java.lang.Throwable { " +
                "return proxy != null ? proxy.doInvoke(new Object[]{param0}) : null;" +
                "}";
        String src = String.format(format, FORWARD_INVOCATION, declaring.getName());
        CtMethod ctMethod = CtMethod.make(src, declaring);
        if (nonNull(apiMethod)) {
            ConstPool constPool = declaring.getClassFile().getConstPool();
            AnnotationsAttribute attribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            Annotation apiMethodAnn = new Annotation(ApiMethod.class.getName(), constPool);
            apiMethodAnn.addMemberValue("value", new StringMemberValue(apiMethod.value(), constPool));
            apiMethodAnn.addMemberValue("notes", new StringMemberValue(apiMethod.notes(), constPool));
            apiMethodAnn.addMemberValue("hidden", new BooleanMemberValue(apiMethod.hidden(), constPool));
            apiMethodAnn.addMemberValue("deprecated", new BooleanMemberValue(apiMethod.deprecated(), constPool));
            ArrayMemberValue paramsArrayMemberValue = new ArrayMemberValue(constPool);
            Annotation apiParamAnn = new Annotation(ApiParam.class.getName(), constPool);
            apiParamAnn.addMemberValue("name", new StringMemberValue(declaring.getSimpleName(), constPool));
            apiParamAnn.addMemberValue("value", new StringMemberValue("Not a real parameter, it is a parameter generated after assembly.", constPool));
            apiParamAnn.addMemberValue("required", new BooleanMemberValue(true, constPool));
            AnnotationMemberValue[] paramsAnns = new AnnotationMemberValue[]{new AnnotationMemberValue(apiParamAnn, constPool)};
            paramsArrayMemberValue.setValue(paramsAnns);
            apiMethodAnn.addMemberValue("params", paramsArrayMemberValue);
            attribute.addAnnotation(apiMethodAnn);
            ctMethod.getMethodInfo().addAttribute(attribute);
        }
        return ctMethod;
    }
}
