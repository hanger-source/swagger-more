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
package com.github.uhfun.swagger.aop;

import com.alibaba.fastjson.JSON;
import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.util.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author uhfun
 */
@EnableAspectJAutoProxy
@Aspect
@Slf4j
@Component
public class AspectRequestLogger {
    @Around("@within(org.springframework.stereotype.Service)")
    public Object log(ProceedingJoinPoint point) throws Throwable {
        MethodSignature methodSignature = ((MethodSignature) point.getSignature());
        if (AnnotatedElementUtils.hasAnnotation(methodSignature.getMethod(), ApiMethod.class)) {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String address = WebUtils.getRemoteAddr(request);
            String className = methodSignature.getDeclaringTypeName();
            String methodName = methodSignature.getMethod().getName();
            String args = JSON.toJSONString(point.getArgs());
            try {
                Object result = point.proceed();
                log.info("[swagger-more] Ip {} Invoking {}.{} with arguments {} return {} ",
                        address, className, methodName, args, JSON.toJSONString(result));
                return result;
            } catch (Throwable e) {
                log.info("[swagger-more] Ip {} Invoking {}.{} with arguments {}",
                        address, className, methodName, args, e);
                throw e;
            }
        }
        return point.proceed();
    }
}
