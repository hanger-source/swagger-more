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
package com.github.uhfun.swagger.webmvc;

import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.dubbo.ServiceBean;
import com.github.uhfun.swagger.util.ProxyClassUtils;
import com.github.uhfun.swagger.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author uhfun
 */
@Slf4j
public class DubboHandlerMethod extends HandlerMethod {

    private final ServiceBean serviceBean;
    private final Method realMethod;

    DubboHandlerMethod(ServiceBean serviceBean, Method method) {
        super(new HandlerMethodProxy(serviceBean, method).buildProxy());
        this.serviceBean = serviceBean;
        realMethod = method;
    }

    public Class<?> getInterfaceType() {
        return serviceBean.getInterfaceClass();
    }

    public Method getRealMethod() {
        return realMethod;
    }

    @Override
    public MethodParameter getReturnType() {
        return new SynthesizingMethodParameter(realMethod, -1);
    }

    public boolean isProxy() {
        return realMethod != super.getMethod();
    }

    public static class HandlerMethodProxy {
        private Object ref;
        private Method method;

        HandlerMethodProxy(ServiceBean serviceBean, Method method) {
            ref = serviceBean.getRef();
            this.method = method;
        }

        HandlerMethod buildProxy() {
            Method methodProxy = method;
            try {
                if (needMergeParams(method)) {
                    Class generatedParamClass = ProxyClassUtils.mergeParametersIntoClass(method);
                    methodProxy = ReflectionUtils.findMethod(generatedParamClass, "invokeForward", generatedParamClass);
                    ReflectionUtils.findMethod(generatedParamClass, "init", HandlerMethodProxy.class).invoke(null, this);
                }
            } catch (Exception e) {
                log.error("Build proxy failed.", e);
            }
            return new HandlerMethod(ref, methodProxy);
        }

        public Object doInvoke(Object param0) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
            if (method.getReturnType() != void.class) {
                return method.invoke(ref, getMethodArgumentValues(param0));
            } else {
                method.invoke(ref, getMethodArgumentValues(param0));
            }
            return null;
        }

        private Object[] getMethodArgumentValues(Object param0) throws NoSuchFieldException, IllegalAccessException {
            ApiMethod apiMethod = AnnotatedElementUtils.findMergedAnnotation(method, ApiMethod.class);
            Object[] args = new Object[method.getParameterCount()];
            if (param0 != null) {
                Parameter[] parameters = method.getParameters();
                for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
                    Parameter p = parameters[i];
                    String paramName = apiMethod == null || apiMethod.params().length < i ? p.getName() : apiMethod.params()[i].name();
                    Field field = param0.getClass().getField(paramName);
                    field.setAccessible(true);
                    args[i] = field.get(param0);
                }
            }
            return args;
        }

        private boolean needMergeParams(Method method) {
            for (Parameter parameter : method.getParameters()) {
                if (TypeUtils.isComplexObjectType(parameter.getType()) && method.getParameterCount() > 1) {
                    return true;
                }
            }
            return false;
        }
    }
}
