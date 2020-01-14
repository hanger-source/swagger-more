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
package com.github.uhfun.swagger.dubbo;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

/**
 * 兼容 alibaba dubbo 和 apache dubbo
 *
 * @author uhfun
 */
public class Supports {

    private static boolean APACHE_DUBBO_SUPPORT;
    private static boolean ALIBABA_DUBBO_SUPPORT;

    static {
        try {
            Class apacheServiceBeanClass = ClassUtils.forName("org.apache.dubbo.config.spring.ServiceBean", ClassUtils.getDefaultClassLoader());
            APACHE_DUBBO_SUPPORT = apacheServiceBeanClass != null;
            Class alibabaServiceBeanClass = ClassUtils.forName("com.alibaba.dubbo.config.spring.ServiceBean", ClassUtils.getDefaultClassLoader());
            ALIBABA_DUBBO_SUPPORT = alibabaServiceBeanClass != null;
        } catch (ClassNotFoundException ignored) {
        }
    }

    public static boolean isServiceBean(Class<?> beanType) {
        return (APACHE_DUBBO_SUPPORT && org.apache.dubbo.config.spring.ServiceBean.class.isAssignableFrom(beanType)) ||
                (ALIBABA_DUBBO_SUPPORT && com.alibaba.dubbo.config.spring.ServiceBean.class.isAssignableFrom(beanType));
    }

    public static boolean hasServiceAnnotation(Class<?> beanType) {
        return (APACHE_DUBBO_SUPPORT && AnnotatedElementUtils.hasAnnotation(beanType, org.apache.dubbo.config.annotation.Service.class)) ||
                (ALIBABA_DUBBO_SUPPORT && AnnotatedElementUtils.hasAnnotation(beanType, com.alibaba.dubbo.config.annotation.Service.class));
    }

    public static ServiceBean wrapServiceBean(Object handler) {
        if (APACHE_DUBBO_SUPPORT) {
            org.apache.dubbo.config.annotation.Service service = AnnotationUtils.findAnnotation(handler.getClass(), org.apache.dubbo.config.annotation.Service.class);
            if (service != null) {
                handler = new org.apache.dubbo.config.spring.ServiceBean(service);
            }
            if (handler instanceof org.apache.dubbo.config.spring.ServiceBean) {
                handler = new ApacheDubboServiceBean((org.apache.dubbo.config.spring.ServiceBean) handler);
            }
        }
        if (ALIBABA_DUBBO_SUPPORT) {
            com.alibaba.dubbo.config.annotation.Service service = AnnotationUtils.findAnnotation(handler.getClass(), com.alibaba.dubbo.config.annotation.Service.class);
            if (service != null) {
                handler = new com.alibaba.dubbo.config.spring.ServiceBean(service);
            }
            if (handler instanceof com.alibaba.dubbo.config.spring.ServiceBean) {
                handler = new AlibabaDubboServiceBean((com.alibaba.dubbo.config.spring.ServiceBean) handler);
            }
        }
        return (ServiceBean) handler;
    }
}
