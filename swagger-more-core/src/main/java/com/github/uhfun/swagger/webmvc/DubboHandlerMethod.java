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

import com.github.uhfun.swagger.dubbo.ServiceBean;
import com.github.uhfun.swagger.util.ClassUtils;
import com.github.uhfun.swagger.util.TypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author uhfun
 */
@Slf4j
public class DubboHandlerMethod extends HandlerMethod {

    private final ServiceBean serviceBean;

    DubboHandlerMethod(ServiceBean serviceBean, Method method) {
        super(serviceBean.getRef(), method);
        this.serviceBean = serviceBean;
    }

    public Class<?> getInterfaceType() {
        return serviceBean.getInterfaceClass();
    }

    public boolean needMergeParams() {
        for (Parameter parameter : getMethod().getParameters()) {
            if (TypeUtils.isComplexObjectType(parameter.getType()) && getMethod().getParameterCount() > 1) {
                return true;
            }
        }
        return false;
    }

    public Class<?> getFakeMethodParameter() {
        return ClassUtils.mergeParametersIntoClass(getMethod());
    }

}
