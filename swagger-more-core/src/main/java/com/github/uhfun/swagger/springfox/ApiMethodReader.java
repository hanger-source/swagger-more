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

import com.fasterxml.classmate.ResolvedType;
import com.github.uhfun.swagger.annotations.ApiMethod;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import springfox.documentation.builders.OperationBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelReference;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;

import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;
import static springfox.documentation.schema.ResolvedTypes.modelRefFactory;
import static springfox.documentation.schema.Types.isVoid;


/**
 * @author unfun
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1001)
public class ApiMethodReader implements OperationBuilderPlugin {

    private final DescriptionResolver resolver;
    private final TypeNameExtractor typeNameExtractor;
    private final DocumentationPluginsManager pluginsManager;

    public ApiMethodReader(DescriptionResolver resolver,
                           TypeNameExtractor typeNameExtractor,
                           DocumentationPluginsManager pluginsManager) {
        this.resolver = resolver;
        this.typeNameExtractor = typeNameExtractor;
        this.pluginsManager = pluginsManager;
    }

    @Override
    public void apply(OperationContext context) {
        Optional<ApiMethod> optional = context.findAnnotation(ApiMethod.class);
        if (optional.isPresent()) {
            OperationBuilder builder = context.operationBuilder();
            ApiMethod apiMethod = optional.get();
            if (StringUtils.hasText(apiMethod.notes())) {
                builder.notes(resolver.resolve(apiMethod.notes()));
            }
            if (StringUtils.hasText(apiMethod.value())) {
                builder.summary(resolver.resolve(apiMethod.value()));
            }
            builder.deprecated(String.valueOf(apiMethod.deprecated()));
            builder.hidden(apiMethod.hidden());
            builder.uniqueId(context.getGroupName() + context.getName());
            builder.method(context.httpMethod());
            builder.consumes(context.consumes().stream().map(MediaType::toString).collect(toSet()));
            builder.produces(context.produces().stream().map(MediaType::toString).collect(toSet()));
            readTags(context);
            readReturnDescription(context, apiMethod);
//            readParameters(context);
        }
    }

    private void readParameters(OperationContext context) {
        List<Parameter> parameterList = Lists.newArrayList();
        for (ResolvedMethodParameter parameter : context.getParameters()) {
            ParameterContext parameterContext = new ParameterContext(parameter,
                    new ParameterBuilder(),
                    context.getDocumentationContext(),
                    context.getGenericsNamingStrategy(),
                    context);
            parameterList.add(pluginsManager.parameter(parameterContext));
        }
        context.operationBuilder().parameters(parameterList);
    }

    private void readReturnDescription(OperationContext context, ApiMethod apiMethod) {
        ResolvedType returnType = context.alternateFor(context.getReturnType());
        String message = StringUtils.isEmpty(apiMethod.returnDescription()) ? "成功" : apiMethod.returnDescription();
        ModelReference modelRef = null;
        if (!isVoid(returnType)) {
            ModelContext modelContext = ModelContext.returnValue(
                    context.getGroupName(), returnType,
                    context.getDocumentationType(),
                    context.getAlternateTypeProvider(),
                    context.getGenericsNamingStrategy(),
                    context.getIgnorableParameterTypes());
            modelRef = modelRefFactory(modelContext, typeNameExtractor).apply(returnType);
        }
        ResponseMessage built = new ResponseMessageBuilder()
                .code(HttpStatus.OK.value())
                .message(message)
                .responseModel(modelRef)
                .build();
        context.operationBuilder().responseMessages(newHashSet(built));
    }

    private void readTags(OperationContext context) {
        Optional<Api> apiOptional = context.findControllerAnnotation(Api.class);
        String defaultTag = context.getGroupName();
        String tag = apiOptional.transform(Api::tags).transform(tags -> tags.length <= 0 || StringUtils.isEmpty(tags[0]) ? defaultTag : tags[0]).or(defaultTag);
        context.operationBuilder().tags(newHashSet(tag));
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}
