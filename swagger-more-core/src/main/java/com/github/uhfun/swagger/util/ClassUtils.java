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

import io.swagger.annotations.ApiModelProperty;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * @author uhfun
 */
@Slf4j
public class ClassUtils {

    public static Class make(String className, Class[] fieldTypes, List<String> names, List<String> values) {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass;
        try {
            ctClass = pool.get(className);
        } catch (NotFoundException notFoundException) {
            ctClass = pool.makeClass(className);
            try {
                for (int i = 0; i < fieldTypes.length; i++) {
                    ctClass.addField(createField(fieldTypes[i], names.get(i), values.get(i), ctClass));
                }
                ctClass.writeFile("target/classes");
                return ctClass.toClass();
            } catch (Exception e) {
                log.error("Dynamically generated class error :", e);
            }
        }
        if (Objects.nonNull(ctClass)) {
            try {
                return pool.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                log.error("ClassNotFoundException : ", e);
            }
        }
        return null;
    }

    private static CtField createField(Class aClass, String name, String value, CtClass ctClass) throws NotFoundException, CannotCompileException {
        ClassPool.getDefault().insertClassPath(new ClassClassPath(aClass));
        CtField field = new CtField(ClassPool.getDefault().get(aClass.getName()), name, ctClass);
        field.setModifiers(javassist.Modifier.PUBLIC);
        ConstPool constPool = ctClass.getClassFile().getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation ann = new Annotation(ApiModelProperty.class.getName(), constPool);
        ann.addMemberValue("value", new StringMemberValue(value, constPool));
        ann.addMemberValue("name", new StringMemberValue(name, constPool));
        ann.addMemberValue("required", new BooleanMemberValue(true, constPool));
        attr.addAnnotation(ann);
        field.getFieldInfo().addAttribute(attr);
        return field;
    }
}
