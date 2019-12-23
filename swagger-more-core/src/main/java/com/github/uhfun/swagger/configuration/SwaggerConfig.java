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

import com.github.uhfun.swagger.common.ExtendRequestHandlerSelectors;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

/**
 * @author fuhangbo
 */
@Configuration
@Import(SpringfoxSupportConfiguration.class)
@ComponentScan(basePackages = "com.github.uhfun.swagger")
public class SwaggerConfig {

    @Bean
    @Autowired
    public Docket complete(ServletContext servletContext) {
        List<ResponseMessage> responseMessageList = newArrayList();
        return new Docket(SWAGGER_2)
                .pathProvider(new RelativePathProvider(servletContext) {
                    @Override
                    public String getApplicationBasePath() {
                        return "/dubbo";
                    }
                })
                .apiInfo(apiInfo())
                .select()
                .apis(ExtendRequestHandlerSelectors.dubboApi())
                .paths(PathSelectors.any())
                .build()
                .groupName("dubbo")
                .produces(Sets.newHashSet("application/json", "text/plain"))
                .consumes(Collections.singleton("application/json"))
                .useDefaultResponseMessages(false)
                .globalResponseMessage(RequestMethod.GET, responseMessageList)
                .globalResponseMessage(RequestMethod.POST, responseMessageList);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Dubbo API")
                .description("核心基于Springfox Swagger2扩展")
                .version("1.0.1-SNAPSHOT")
                .build();
    }
}
