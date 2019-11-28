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
package com.github.uhfun.swagger.web;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.common.SwaggerMoreException;
import com.github.uhfun.swagger.util.WebUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.fastjson.parser.Feature.*;
import static com.github.uhfun.swagger.util.TypeUtils.isComplexObjectType;
import static com.github.uhfun.swagger.util.TypeUtils.isContainerType;
import static java.util.Objects.nonNull;

/**
 * @author uhfun
 */
@Slf4j
@Controller
public class ApiInvokeController {

    @GetMapping("/api/dubbo")
    public String dubboApi() {
        return "/api/index.html";
    }

    @PostMapping(value = "/dubbo/{classSimpleName}/{methodName}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Object invoke(@PathVariable String classSimpleName,
                         @PathVariable String methodName,
                         HttpServletRequest request) throws IOException, InvocationTargetException, IllegalAccessException {
        Object target = ApplicationHolder.getBean(classSimpleName.substring(0, 1).toLowerCase() + classSimpleName.substring(1));
        Object actualTarget = AopUtils.isAopProxy(target) ? AopProxyUtils.getSingletonTarget(target) : target;
        String invokeId = UUID.randomUUID().toString();
        log.info("[swagger-more] 调用({}): ip: {}, invoke -> {}.{}", invokeId, WebUtils.getRemoteAddr(request), actualTarget.getClass(), methodName);
        Method method = getMethod(methodName, actualTarget.getClass());
        Enumeration enumeration = request.getParameterNames();
        JSONObject object = new JSONObject();
        ApiMethod apiMethod = AnnotationUtils.findAnnotation(method, ApiMethod.class);
        Map<String, Parameter> parameterMap = getParameterMap(nonNull(apiMethod) ? Stream.of(apiMethod.params()).map(ApiParam::name).collect(Collectors.toList()) : Lists.newArrayList(), method);
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();
            List<Object> values = Stream.of(request.getParameterValues(name))
                    .map(o -> o.startsWith("[") || o.startsWith("{") ? JSONObject.parseObject(o, SupportAutoType, OrderedField) : o).collect(Collectors.toList());
            object.put(name, Objects.isNull(parameterMap.get(name)) || !isContainerType(parameterMap.get(name).getType()) ? values.get(0) : values);
        }
        List<Object> params = Lists.newArrayList();
        String body = getBodyString(request);
        String payload = StringUtils.isEmpty(body) ? object.toJSONString() : body;
        if (method.getParameterTypes().length == 1 && isComplexObjectType(method.getParameterTypes()[0])) {
            params.add(JSONObject.parseObject(payload, method.getParameters()[0].getParameterizedType(), SupportAutoType, OrderedField));
        } else {
            parameterMap.keySet().forEach((name) -> {
                String arg = JSONObject.parseObject(payload, DisableSpecialKeyDetect, OrderedField).getString(name);
                params.add(StringUtils.isEmpty(arg) ? null : arg.startsWith("[") || arg.startsWith("{")
                        ? JSONObject.parseObject(arg, parameterMap.get(name).getParameterizedType(), SupportAutoType, OrderedField)
                        : TypeUtils.cast(arg, parameterMap.get(name).getParameterizedType(), ParserConfig.getGlobalInstance()));
            });
        }
        log.info("[swagger-more] 调用({}): 入参: {}", invokeId, JSONObject.toJSONString(params));
        Object result = method.invoke(target, params.toArray());
        return method.getReturnType().equals(void.class) ? "成功" : Objects.isNull(result) ? "null" : result;
    }

    private Method getMethod(String methodName, Class<?> clazz) {
        Optional<Method> methodOptional;
        if (methodName.contains("_(")) {
            String[] methodStr = methodName.split("_");
            methodOptional = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.getName().equals(methodStr[0]))
                    .filter(m -> ("(" + Stream.of(m.getParameters())
                            .map(p -> p.getType().getSimpleName())
                            .collect(Collectors.joining(", ")) + ")").equals(methodStr[1]))
                    .findAny();
        } else {
            methodOptional = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .findAny();
        }
        return methodOptional.orElseThrow(() -> new SwaggerMoreException("找不到方法: " + methodName));
    }

    private Map<String, Parameter> getParameterMap(List<String> names, Method method) {
        Map<String, Parameter> map = Maps.newLinkedHashMap();
        if (names.isEmpty()) {
            Stream.of(method.getParameters()).forEach(parameter -> map.put(parameter.getName().replace("arg", "param"), parameter));
            return map;
        }
        for (int i = 0; i < names.size(); i++) {
            map.put(names.get(i), method.getParameters()[i]);
        }
        return map;
    }

    private String getBodyString(HttpServletRequest request) throws IOException {
        String str;
        StringBuilder wholeStr = new StringBuilder();
        while ((str = request.getReader().readLine()) != null) {
            wholeStr.append(str);
        }
        return wholeStr.toString();
    }
}
