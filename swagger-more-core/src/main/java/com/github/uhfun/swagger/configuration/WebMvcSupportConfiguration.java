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
package com.github.uhfun.swagger.configuration;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.github.uhfun.swagger.aop.AspectRequestLogger;
import com.github.uhfun.swagger.webmvc.DubboHandlerMethodMapping;
import com.github.uhfun.swagger.webmvc.DubboMethodMessageConverterResolver;
import com.github.uhfun.swagger.webmvc.ExceptionMessages;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ViewNameMethodReturnValueHandler;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.Map;

import static com.alibaba.fastjson.parser.Feature.*;
import static java.util.Collections.singletonList;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * @author uhfun
 */
@Import(AspectRequestLogger.class)
public class WebMvcSupportConfiguration extends WebMvcConfigurerAdapter {

    private static final DubboMethodMessageConverterResolver DUBBO_METHOD_MESSAGE_CONVERTER_RESOLVER =
            new DubboMethodMessageConverterResolver(singletonList(fastJsonHttpMessageConverter()));

    // The following converters are used

    private static FastJsonHttpMessageConverter fastJsonHttpMessageConverter() {
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
                SerializerFeature.PrettyFormat,
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.DisableCircularReferenceDetect,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.WriteDateUseDateFormat);
        config.setFeatures(DisableSpecialKeyDetect, SupportAutoType, OrderedField);
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        converter.setFastJsonConfig(config);
        converter.setSupportedMediaTypes(Lists.newArrayList(MediaType.APPLICATION_JSON_UTF8));
        return converter;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        registry.viewResolver(resolver);
        registry.order(1);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/api/**")
                .addResourceLocations("classpath:/META-INF/resources/static/");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(DUBBO_METHOD_MESSAGE_CONVERTER_RESOLVER);
    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        returnValueHandlers.add(DUBBO_METHOD_MESSAGE_CONVERTER_RESOLVER);
    }

    // Following are ExceptionHandlers

    @Bean
    public ExceptionMessages exceptionMessages() {
        return new ExceptionMessages();
    }

    // Followings are custom HandlerMappings

    @Bean
    public DubboHandlerMethodMapping dubboHandlerMethodMapping(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        requestMappingHandlerAdapter.setReturnValueHandlers(adjustHandlerMethodReturnValueHandlerOrder(requestMappingHandlerAdapter.getReturnValueHandlers()));
        return new DubboHandlerMethodMapping();
    }

    private List<HandlerMethodReturnValueHandler> adjustHandlerMethodReturnValueHandlerOrder(List<HandlerMethodReturnValueHandler> handlerMethodReturnValueHandlers) {
        List<HandlerMethodReturnValueHandler> handlers = Lists.newArrayList();
        for (HandlerMethodReturnValueHandler returnValueHandler : handlerMethodReturnValueHandlers) {
            // Methods that return void will be handled by ViewNameMethodReturnValueHandler, change the order
            if (returnValueHandler instanceof ViewNameMethodReturnValueHandler) {
                handlers.add(DUBBO_METHOD_MESSAGE_CONVERTER_RESOLVER);
            }
            handlers.add(returnValueHandler);
        }
        return handlers;
    }

    @Bean
    public HandlerMapping dubboApiIndexHandlerMapping() {
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        Map<String, Object> map = Maps.newHashMap();
        HandlerMethod handlerMethod = new HandlerMethod(this, ReflectionUtils.findMethod(getClass(), "apiIndexHtml"));
        map.put("/api/dubbo", handlerMethod);
        handlerMapping.setUrlMap(map);
        handlerMapping.setOrder(HIGHEST_PRECEDENCE);
        return handlerMapping;
    }

    public String apiIndexHtml() {
        // api index controller
        return "/api/index.html";
    }
}
