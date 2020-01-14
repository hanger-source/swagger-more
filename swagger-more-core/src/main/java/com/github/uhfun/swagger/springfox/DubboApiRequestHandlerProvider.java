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

import com.fasterxml.classmate.TypeResolver;
import com.github.uhfun.swagger.webmvc.DubboHandlerMethod;
import com.github.uhfun.swagger.webmvc.DubboHandlerMethodMapping;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import springfox.documentation.RequestHandler;
import springfox.documentation.spi.service.RequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.FluentIterable.from;
import static springfox.documentation.builders.BuilderDefaults.nullToEmptyList;
import static springfox.documentation.spi.service.contexts.Orderings.byPatternsCondition;

/**
 * @author uhfun
 */
public class DubboApiRequestHandlerProvider implements RequestHandlerProvider, InitializingBean, ApplicationContextAware {

    private final HandlerMethodResolver methodResolver;
    private final TypeResolver typeResolver;
    private List<DubboHandlerMethodMapping> methodMappings = Lists.newArrayList();
    private ApplicationContext applicationContext;

    public DubboApiRequestHandlerProvider(HandlerMethodResolver methodResolver,
                                          TypeResolver typeResolver) {
        this.methodResolver = methodResolver;
        this.typeResolver = typeResolver;
    }

    @Override
    public List<RequestHandler> requestHandlers() {
        return byPatternsCondition().sortedCopy(from(nullToEmptyList(methodMappings))
                .transformAndConcat(toMappingEntries())
                .transform(toRequestHandler()));
    }

    private com.google.common.base.Function<? super DubboHandlerMethodMapping,
            Iterable<Map.Entry<RequestMappingInfo, HandlerMethod>>> toMappingEntries() {
        return (com.google.common.base.Function<DubboHandlerMethodMapping, Iterable<Map.Entry<RequestMappingInfo, HandlerMethod>>>) input -> input.getHandlerMethods().entrySet();
    }

    private Function<Map.Entry<RequestMappingInfo, HandlerMethod>, RequestHandler> toRequestHandler() {
        return input -> new DubboApiRequestHandler(methodResolver, typeResolver, (DubboHandlerMethod) input.getValue(), input.getKey());
    }

    @Override
    public void afterPropertiesSet() {
        methodMappings.addAll(applicationContext.getBeansOfType(DubboHandlerMethodMapping.class).values());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
