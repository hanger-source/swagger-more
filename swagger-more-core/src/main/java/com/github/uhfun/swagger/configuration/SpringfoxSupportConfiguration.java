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

import com.fasterxml.classmate.TypeResolver;
import com.github.uhfun.swagger.springfox.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.plugin.core.config.EnablePluginRegistries;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.service.PathDecorator;
import springfox.documentation.spi.service.*;
import springfox.documentation.spi.service.contexts.Defaults;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.ObjectMapperConfigurer;
import springfox.documentation.spring.web.json.JacksonModuleRegistrar;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;
import springfox.documentation.spring.web.scanners.MediaTypeReader;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import springfox.documentation.swagger2.configuration.Swagger2DocumentationConfiguration;
import springfox.documentation.swagger2.configuration.Swagger2JacksonModule;

import java.util.List;

/**
 * @author uhfun
 */
@Configuration
@ComponentScans({
        @ComponentScan(basePackages = {
                "springfox.documentation.swagger.web",
                "springfox.documentation.swagger2.web",
                "springfox.documentation.schema",
                "springfox.documentation.swagger.schema",
                "springfox.documentation.swagger2.mappers",
                "springfox.documentation.spring.web.plugins"}),
        @ComponentScan(basePackages = "springfox.documentation.spring.web.plugins",
                excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcRequestHandlerProvider.class)),
        @ComponentScan(basePackages = "springfox.documentation.spring.web.scanners",
                excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MediaTypeReader.class)),
        @ComponentScan(basePackages = "springfox.documentation.spring.web.readers.operation",
                excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "^((?!(ApiOperationReader|DefaultOperationReader|OperationParameterReader|CachingOperationNameGenerator)).)*$")),
        @ComponentScan(basePackages = "springfox.documentation.spring.web.readers.parameter",
                excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "^((?!(ModelAttributeParameterExpander|ExpandedParameterBuilder|ParameterDataTypeReader)).)*$"))
})
@EnablePluginRegistries({
        DocumentationPlugin.class,
        ApiListingBuilderPlugin.class,
        OperationBuilderPlugin.class,
        ParameterBuilderPlugin.class,
        ExpandedParameterBuilderPlugin.class,
        ResourceGroupingStrategy.class,
        OperationModelsProviderPlugin.class,
        DefaultsProviderPlugin.class,
        PathDecorator.class,
        ApiListingScannerPlugin.class
})
@Conditional({SpringfoxSupportConfiguration.EnableSwagger2MissingConditional.class})
@ConditionalOnMissingBean(annotation = EnableSwagger2.class)
public class SpringfoxSupportConfiguration {

    @Bean
    public static ObjectMapperConfigurer objectMapperConfigurer() {
        return new ObjectMapperConfigurer();
    }

    @Bean
    public Defaults defaults() {
        return new Defaults();
    }

    @Bean
    public DocumentationCache resourceGroupCache() {
        return new DocumentationCache();
    }

    @Bean
    public JsonSerializer jsonSerializer(List<JacksonModuleRegistrar> moduleRegistrars) {
        return new JsonSerializer(moduleRegistrars);
    }

    @Bean
    public JacksonModuleRegistrar swagger2Module() {
        return new Swagger2JacksonModule();
    }

    @Bean
    public DescriptionResolver descriptionResolver(Environment environment) {
        return new DescriptionResolver(environment);
    }

    @Bean
    public HandlerMethodResolver methodResolver(TypeResolver resolver) {
        return new HandlerMethodResolver(resolver);
    }

    // Followings are custom plugins

    @Bean
    public DubboApiRequestHandlerProvider apiRequestHandlerProvider(HandlerMethodResolver methodResolver, TypeResolver typeResolver) {
        return new DubboApiRequestHandlerProvider(methodResolver, typeResolver);
    }

    @Bean
    public ApiParamReader apiParamReader(DescriptionResolver resolver) {
        return new ApiParamReader(resolver);
    }

    @Bean
    public ApiTagReader apiTagReader() {
        return new ApiTagReader();
    }

    @Bean
    public ApiMethodReader apiMethodReader(DescriptionResolver resolver, TypeNameExtractor typeNameExtractor, DocumentationPluginsManager pluginsManager) {
        return new ApiMethodReader(resolver, typeNameExtractor, pluginsManager);
    }

    @Bean
    public ApiMethodModelsProvider apiMethodModelsProvider(TypeResolver typeResolver) {
        return new ApiMethodModelsProvider(typeResolver);
    }

    @Bean
    public ModelExtendsBuilder modelExtendsBuilder() {
        return new ModelExtendsBuilder();
    }

    static final class EnableSwagger2MissingConditional implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // Non-spring Boot applications do not add @EnableSwagger2 to enable this configuration
            return !context.getRegistry().containsBeanDefinition(Swagger2DocumentationConfiguration.class.getName());
        }
    }
}
