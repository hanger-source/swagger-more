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
package com.github.uhfun.swagger.extension;

import com.alibaba.dubbo.config.spring.ServiceBean;
import com.fasterxml.classmate.TypeResolver;
import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.common.SwaggerMoreException;
import com.github.uhfun.swagger.util.ClassUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import springfox.documentation.RequestHandler;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.service.RequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.uhfun.swagger.common.Constant.*;
import static com.github.uhfun.swagger.util.TypeUtils.isComplexObjectType;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static springfox.documentation.builders.BuilderDefaults.nullToEmptyList;
import static springfox.documentation.spi.service.contexts.Orderings.byPatternsCondition;

/**
 * @author uhfun
 */
@Component
@ApiRequestHandlerProvider.Body
public class ApiRequestHandlerProvider implements RequestHandlerProvider {

    private final List<ServiceBean> serviceBeans;
    private final HandlerMethodResolver methodResolver;
    private final TypeResolver typeResolver;

    @Autowired
    public ApiRequestHandlerProvider(List<ServiceBean> serviceBeans,
                                     HandlerMethodResolver methodResolver,
                                     TypeResolver typeResolver) {
        this.serviceBeans = serviceBeans;
        this.methodResolver = methodResolver;
        this.typeResolver = typeResolver;
    }

    @Override
    public List<RequestHandler> requestHandlers() {
        return byPatternsCondition().sortedCopy(nullToEmptyList(serviceBeans).stream()
                .filter(bean -> AnnotatedElementUtils.hasAnnotation(bean.getInterfaceClass(), Api.class))
                .reduce(newArrayList(), toMappingEntries(), (o1, o2) -> o1)
                .stream().map(toRequestHandler()).collect(Collectors.toList()));
    }

    private BiFunction<List<HandlerMethod>, ? super ServiceBean,
            List<HandlerMethod>> toMappingEntries() {
        return (list, bean) -> {
            Object object = AopUtils.isAopProxy(bean.getRef())
                    ? AopProxyUtils.getSingletonTarget(bean.getRef()) : bean.getRef();
            list.addAll(Arrays.stream(bean.getInterfaceClass().getDeclaredMethods())
                    .filter(method -> !Modifier.isStatic(method.getModifiers()))
                    .filter(method -> AnnotatedElementUtils.hasAnnotation(method, ApiMethod.class))
                    .map(method -> new HandlerMethod(object, method))
                    .collect(Collectors.toList()));
            return list;
        };
    }

    private Function<HandlerMethod, RequestHandler> toRequestHandler() {
        return handlerMethod -> new ApiRequestHandler(methodResolver, handlerMethod,
                allAsRequestBody(handlerMethod)
                        ? annotateBody(handlerMethod)
                        : methodResolver.methodParameters(handlerMethod));
    }

    private List<ResolvedMethodParameter> annotateBody(HandlerMethod handlerMethod) {
        ResolvedMethodParameter param0;
        List<Class> parameters = newArrayList(handlerMethod.getMethod().getParameterTypes());
        if (parameters.size() == 1) {
            param0 = methodResolver.methodParameters(handlerMethod).get(0);
        } else {
            Class<?> generatedType = mergeIntoGeneratedType(parameters, handlerMethod.getMethod());
            param0 = new ResolvedMethodParameter(generatedType.getSimpleName(),
                    new MethodParameter(handlerMethod.getMethod(), 0),
                    typeResolver.resolve(generatedType));
        }
        return singletonList(param0.annotate(AnnotationUtils.findAnnotation(getClass(), Body.class).body()));
    }

    private Class<?> mergeIntoGeneratedType(List<Class> parameters, Method method) {
        ApiMethod apiMethod = AnnotationUtils.findAnnotation(method, ApiMethod.class);
        if (isNull(apiMethod)) {
            throw new SwaggerMoreException("Method " + method.getDeclaringClass().getName() + "." + method.getName() + " has more than two complex parameters that must be annotated @ApiMethod with @ApiParam");
        }
        String className = DEFAULT_PACKAGE_NAME +
                method.getDeclaringClass().getSimpleName() + DOT +
                GENERATED_PREFIX + method.getName() + UNDERLINE +
                parameters.stream().map(Class::getSimpleName).collect(joining("_")) +
                DEFAULT_COMPLEX_OBJECT_SUFFIX;
        List<String> names = newArrayList();
        List<String> values = newArrayList();
        for (ApiParam param : apiMethod.params()) {
            names.add(param.name());
            values.add(param.value());
        }
        return ClassUtils.make(className, method.getParameterTypes(), names, values);
    }

    private boolean allAsRequestBody(HandlerMethod handlerMethod) {
        return Stream.of(handlerMethod.getMethod().getParameters()).anyMatch(p -> isComplexObjectType(p.getType()));
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Body {
        RequestBody body() default @RequestBody;
    }
}
