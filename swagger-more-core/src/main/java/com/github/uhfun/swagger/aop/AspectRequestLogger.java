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
 * AopSupportConfiguration
 *
 * @author uhfun
 * @date 2019-12-29  22:19
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
