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

import com.alibaba.fastjson.JSONException;
import com.github.uhfun.swagger.common.SwaggerMoreException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.InvocationTargetException;

import static org.springframework.http.HttpStatus.*;

/**
 * @author uhfun
 */
@Slf4j
@ControllerAdvice(basePackageClasses = ApiInvokeController.class)
@ResponseBody
public class ExceptionMessages {

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    public Object throwable(Throwable e) {
        log.error("内部异常: ", e);
        return "内部异常, 联系Swagger开发人员: " + e.getMessage();
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(JSONException.class)
    public Object jsonParseException(JSONException e) {
        return "Json解析出错, 检查格式是否正确, 错误信息: " + e.getMessage();
    }

    @ResponseStatus(OK)
    @ExceptionHandler(InvocationTargetException.class)
    public Object invocationTargetException(InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        log.error("", e);
        return "异常: throw new " + targetException.getClass().getSimpleName()
                + "(\"" + e.getTargetException().getMessage() + "\");";
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(NoSuchBeanDefinitionException.class)
    public Object noSuchBeanDefinitionException(NoSuchBeanDefinitionException e) {
        return "找不到Bean对象, name: " + e.getBeanName() + "error: " + e.getMessage();
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SwaggerMoreException.class)
    public Object swaggerMoreException(SwaggerMoreException e) {
        return e.getMessage();
    }

}
