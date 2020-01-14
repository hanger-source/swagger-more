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

import com.github.uhfun.swagger.common.DubboMethodHandlerSelectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

import static com.github.uhfun.swagger.common.Constant.BATH_PATH;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

/**
 * @author uhfun
 */
@Import(SpringfoxSupportConfiguration.class)
@ComponentScan(basePackages = "com.souche.swagger.more")
public class SwaggerConfiguration {

    @Bean
    public Docket complete() {
        List<ResponseMessage> responseMessageList = newArrayList();
        return new Docket(SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(DubboMethodHandlerSelectors.dubboApi())
                .paths(PathSelectors.ant(BATH_PATH + "/**"))
                .build()
                .groupName("dubbo")
                .produces(newHashSet(MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.TEXT_PLAIN_VALUE))
                .consumes(singleton(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .useDefaultResponseMessages(false)
                .globalResponseMessage(RequestMethod.GET, responseMessageList)
                .globalResponseMessage(RequestMethod.POST, responseMessageList);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Dubbo API")
                .description("核心基于Springfox Swagger2扩展")
                .version("1.0.2-SNAPSHOT")
                .build();
    }
}
