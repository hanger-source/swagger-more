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
import com.github.uhfun.swagger.dubbo.Supports;
import com.github.uhfun.swagger.util.TypeUtils;
import io.swagger.annotations.Api;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.uhfun.swagger.common.Constant.BATH_PATH;
import static java.util.stream.Collectors.joining;

/**
 * @author uhfun
 */
public class DubboHandlerMethodMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

    @Override
    protected boolean isHandler(Class<?> beanType) {
        return Supports.isServiceBean(beanType) || Supports.hasServiceAnnotation(beanType);
    }

    @Override
    protected void detectHandlerMethods(Object handler) {
        ServiceBean serviceBean = transformToServiceBean(handler);
        Class<?> interfaceType = ClassUtils.getUserClass(serviceBean.getInterfaceClass());
        Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(interfaceType,
                (MethodIntrospector.MetadataLookup<RequestMappingInfo>) method -> getMappingForMethod(method, interfaceType));
        if (logger.isDebugEnabled()) {
            logger.debug(methods.size() + " dubbo handler methods found on " + interfaceType + ": " + methods);
        }
        for (Map.Entry<Method, RequestMappingInfo> entry : methods.entrySet()) {
            Method invocableMethod = AopUtils.selectInvocableMethod(entry.getKey(), interfaceType);
            RequestMappingInfo mapping = entry.getValue();
            registerHandlerMethod(serviceBean, invocableMethod, mapping);
        }
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> interfaceType) {
        if (!Modifier.isStatic(method.getModifiers())
                && Modifier.isPublic(method.getModifiers())
                && support(method, interfaceType)) {
            return createRequestMappingInfo(method, interfaceType);
        }
        return null;
    }

    @Override
    protected HandlerMethod createHandlerMethod(Object handler, Method method) {
        return new DubboHandlerMethod((ServiceBean) handler, method);
    }

    @Override
    protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
        return info.getPatternsCondition().getPatterns();
    }

    @Override
    protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, HttpServletRequest request) {
        return info.getMatchingCondition(request);
    }

    @Override
    protected Comparator<RequestMappingInfo> getMappingComparator(HttpServletRequest request) {
        return (info1, info2) -> info1.compareTo(info2, request);
    }

    private ServiceBean transformToServiceBean(Object handler) {
        if (handler instanceof String) {
            String beanName = (String) handler;
            handler = getApplicationContext().getBean(beanName);
        }
        return Supports.wrapServiceBean(handler);
    }

    private RequestMappingInfo createRequestMappingInfo(Method method, Class<?> interfaceType) {
        String beanName = interfaceType.getSimpleName().substring(0, 1).toLowerCase() + interfaceType.getSimpleName().substring(1);
        RequestMappingInfo.Builder builder = RequestMappingInfo
                .paths(BATH_PATH + "/" + beanName + "/" + getUniqueMethodName(method, interfaceType))
                .methods(RequestMethod.POST)
                .consumes(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .produces(MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.TEXT_PLAIN_VALUE);
        supportMethods(method, builder);
        return builder.build();
    }

    private boolean support(Method method, Class interfaceType) {
        return AnnotatedElementUtils.hasAnnotation(interfaceType, Api.class) &&
                AnnotatedElementUtils.hasAnnotation(method, ApiMethod.class);
    }

    private void supportMethods(Method method, RequestMappingInfo.Builder builder) {
        if (Stream.of(method.getParameterTypes()).noneMatch(TypeUtils::isComplexObjectType)) {
            builder.methods(RequestMethod.GET, RequestMethod.POST)
                    .consumes(MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        }
    }

    private String getUniqueMethodName(Method method, Class<?> interfaceType) {
        return Stream.of(interfaceType.getDeclaredMethods())
                .filter(m -> m.getName().equals(method.getName()))
                .count() == 1
                ? method.getName()
                : method.getName() + "_(" +
                Stream.of(method.getParameters())
                        .map(Parameter::getType)
                        .map(Class::getSimpleName)
                        .collect(joining(", "))
                + ")";
    }
}
