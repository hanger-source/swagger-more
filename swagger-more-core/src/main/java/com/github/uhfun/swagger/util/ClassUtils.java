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
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;

import static com.github.uhfun.swagger.common.Constant.*;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

/**
 * @author uhfun
 */
@Slf4j
public class ClassUtils {

    public static Class mergeParametersIntoClass(Method method) {
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
}
