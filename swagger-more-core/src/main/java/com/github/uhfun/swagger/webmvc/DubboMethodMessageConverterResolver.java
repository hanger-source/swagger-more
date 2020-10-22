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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.util.TypeUtils;
import io.swagger.annotations.ApiParam;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static com.alibaba.fastjson.parser.Feature.*;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;

/**
 * @author uhfun
 */
public class DubboMethodMessageConverterResolver implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

    private final RequestResponseBodyMethodProcessor responseBodyMethodProcessor;
    private final ApiParamNameValueMethodArgumentResolver apiParamNameValueMethodArgumentResolver;

    public DubboMethodMessageConverterResolver(List<HttpMessageConverter<?>> converters) {
        responseBodyMethodProcessor = new RequestResponseBodyMethodProcessor(converters, emptyList());
        apiParamNameValueMethodArgumentResolver = new ApiParamNameValueMethodArgumentResolver();
    }

    // handle returnValue

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return support(parameter);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        if (parameter.getMethod().getParameterCount() == 1 && TypeUtils.isComplexObjectType(parameter.getParameterType())) {
            return responseBodyMethodProcessor.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        } else {
            return apiParamNameValueMethodArgumentResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        }
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return support(returnType);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        if (returnValue == null) {
            returnValue = returnType.getParameterType() == void.class ? "success" : new JSONObject();
        }
        responseBodyMethodProcessor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
    }

    private boolean support(MethodParameter parameter) {
        return parameter.hasMethodAnnotation(ApiMethod.class);
    }

    static class ApiParamNameValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

        private static final String BODY_VALUE_ATTRIBUTE = HandlerMapping.class.getName() + ".bodyValueParams";

        @Override
        protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
            int index = parameter.getParameterIndex();
            ApiMethod apiMethod = parameter.getMethodAnnotation(ApiMethod.class);
            if (nonNull(apiMethod) && apiMethod.params().length >= index) {
                return new ApiParamNamedValueInfo(apiMethod.params()[index]);
            }
            return new NamedValueInfo(parameter.getParameterName(), false, null);
        }

        @Override
        protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws IOException {
            Object paramValue = getParameterValues(name, request);
            if (paramValue == null) {
                return null;
            }
            boolean isStringArray = String[].class == paramValue.getClass();
            if (isStringArray && ((String[]) paramValue).length == 0) {
                return null;
            }
            if (TypeUtils.isBaseType(parameter.getParameterType())) {
                paramValue = isStringArray ? ((String[]) paramValue)[0] : paramValue;
                return com.alibaba.fastjson.util.TypeUtils.cast(paramValue, parameter.getGenericParameterType(), ParserConfig.getGlobalInstance());
            }
            if (TypeUtils.isContainerType(parameter.getParameterType())) {
                paramValue = isStringArray ? "[" + String.join(",", (String[]) paramValue) + "]" : paramValue;
            }
            return JSONObject.parseObject((String) paramValue, parameter.getGenericParameterType(), SupportAutoType, OrderedField);
        }

        private Object getParameterValues(String name, NativeWebRequest request) throws IOException {
            Object values = request.getParameterValues(name);
            if (values == null) {
                JSONObject body = (JSONObject) request.getAttribute(BODY_VALUE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                if (body == null) {
                    HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
                    body = JSONObject.parseObject(readBody(servletRequest), DisableSpecialKeyDetect, OrderedField);
                    request.setAttribute(BODY_VALUE_ATTRIBUTE, body, RequestAttributes.SCOPE_REQUEST);
                }
                if (body == null) {
                    return null;
                }
                Object param = body.get(name);
                if (param instanceof JSONArray) {
                    return ((JSONArray) param).toJSONString();
                }
                return body.getString(name);
            }
            return values;
        }

        private String readBody(ServletRequest request) throws IOException {
            String str;
            StringBuilder wholeStr = new StringBuilder();
            while ((str = request.getReader().readLine()) != null) {
                wholeStr.append(str);
            }
            return wholeStr.toString();
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return true;
        }

        static class ApiParamNamedValueInfo extends NamedValueInfo {

            ApiParamNamedValueInfo(ApiParam apiParam) {
                super(apiParam.name(), apiParam.required(), null);
            }
        }
    }
}
