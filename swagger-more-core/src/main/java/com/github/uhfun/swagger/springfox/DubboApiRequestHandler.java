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
package com.github.uhfun.swagger.springfox;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.github.uhfun.swagger.util.TypeUtils;
import com.github.uhfun.swagger.webmvc.DubboHandlerMethod;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import springfox.documentation.RequestHandler;
import springfox.documentation.RequestHandlerKey;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static springfox.documentation.spring.web.paths.Paths.splitCamelCase;

/**
 * @author uhfun
 */
@Slf4j
public class DubboApiRequestHandler implements RequestHandler {

    @RequestBodyHolder
    private static RequestBody REQUEST_BODY_ANN;

    static {
        Field field = ReflectionUtils.findField(DubboApiRequestHandler.class, "REQUEST_BODY_ANN");
        REQUEST_BODY_ANN = AnnotationUtils.findAnnotation(field, RequestBodyHolder.class).value();
    }

    private final HandlerMethodResolver methodResolver;
    private final TypeResolver typeResolver;
    private final DubboHandlerMethod handlerMethod;
    private final RequestMappingInfo requestMapping;
    private List<ResolvedMethodParameter> parameters;

    DubboApiRequestHandler(HandlerMethodResolver methodResolver,
                           TypeResolver typeResolver,
                           DubboHandlerMethod handlerMethod,
                           RequestMappingInfo requestMapping) {
        this.methodResolver = methodResolver;
        this.typeResolver = typeResolver;
        this.handlerMethod = handlerMethod;
        this.requestMapping = requestMapping;
    }

    @Override
    public Class<?> declaringClass() {
        return handlerMethod.getBeanType();
    }

    @Override
    public boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
        return null != AnnotationUtils.findAnnotation(handlerMethod.getMethod(), annotation);
    }

    @Override
    public PatternsRequestCondition getPatternsCondition() {
        return requestMapping.getPatternsCondition();
    }

    @Override
    public String groupName() {
        return splitCamelCase(handlerMethod.getInterfaceType().getSimpleName(), "-").replace("/", "").toLowerCase();
    }

    @Override
    public String getName() {
        Method method = handlerMethod.getRealMethod();
        return Stream.of(method.getDeclaringClass().getDeclaredMethods())
                .filter(m -> m.getName().equals(method.getName()))
                .count() == 1
                ? method.getName()
                : method.getName() +
                "_(" + Stream.of(method.getParameters()).map(Parameter::getType)
                .map(Class::getSimpleName).collect(joining(", ")) + ")";
    }

    @Override
    public Set<RequestMethod> supportedMethods() {
        return Sets.newHashSet(RequestMethod.POST);
    }

    @Override
    public Set<? extends MediaType> produces() {
        return requestMapping.getProducesCondition().getProducibleMediaTypes();
    }

    @Override
    public Set<? extends MediaType> consumes() {
        return requestMapping.getConsumesCondition().getConsumableMediaTypes();
    }

    @Override
    public Set<NameValueExpression<String>> headers() {
        return Sets.newHashSet();
    }

    @Override
    public Set<NameValueExpression<String>> params() {
        return Sets.newHashSet();
    }

    @Override
    public <T extends Annotation> Optional<T> findAnnotation(Class<T> annotation) {
        return Optional.fromNullable(AnnotationUtils.findAnnotation(handlerMethod.getMethod(), annotation));
    }

    @Override
    public RequestHandlerKey key() {
        return new RequestHandlerKey(getPatternsCondition().getPatterns(), supportedMethods(), consumes(), produces());
    }

    @Override
    public List<ResolvedMethodParameter> getParameters() {
        if (parameters == null) {
            if (handlerMethod.isProxy()) {
                MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];
                ResolvedType resolvedType = typeResolver.resolve(methodParameter.getParameterType());
                parameters = singletonList(new ResolvedMethodParameter("param0", methodParameter, resolvedType)
                        .annotate(REQUEST_BODY_ANN));
            } else {
                parameters = methodResolver.methodParameters(handlerMethod);
                ResolvedMethodParameter param0;
                if (parameters.size() == 1 && TypeUtils.isComplexObjectType((param0 = parameters.get(0)).getParameterType().getErasedType())) {
                    parameters.set(0, param0.annotate(REQUEST_BODY_ANN));
                }
            }
        }
        return parameters;
    }

    @Override
    public ResolvedType getReturnType() {
        return typeResolver.resolve(handlerMethod.getReturnType().getGenericParameterType());
    }

    @Override
    public <T extends Annotation> Optional<T> findControllerAnnotation(Class<T> annotation) {
        return Optional.fromNullable(AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), annotation));
    }

    @Override
    public RequestMappingInfo getRequestMapping() {
        return requestMapping;
    }

    @Override
    public HandlerMethod getHandlerMethod() {
        return handlerMethod;
    }

    @Override
    public RequestHandler combine(RequestHandler other) {
        return this;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("DubboApiRequestHandler{");
        sb.append("key=").append(key());
        sb.append('}');
        return sb.toString();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface RequestBodyHolder {
        RequestBody value() default @RequestBody;
    }
}
