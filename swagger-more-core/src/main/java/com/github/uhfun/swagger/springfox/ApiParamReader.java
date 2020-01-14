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
import com.github.uhfun.swagger.common.SwaggerMoreException;
import com.github.uhfun.swagger.util.TypeUtils;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.Example;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.service.StringVendorExtension;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spring.web.DescriptionResolver;

import java.util.List;

import static com.google.common.base.Strings.emptyToNull;
import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;
import static springfox.documentation.swagger.readers.parameter.Examples.examples;

/**
 * @author uhfun
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1001)
public class ApiParamReader implements ParameterBuilderPlugin {

    private final DescriptionResolver resolver;

    public ApiParamReader(DescriptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void apply(ParameterContext context) {
        ResolvedType resolvedType = context.resolvedMethodParameter().getParameterType();
        Class erasedType = resolvedType.getErasedType();
        context.parameterBuilder()
                .name(readName(context.resolvedMethodParameter()))
                .description(resolvedType.getTypeName())
                .parameterType("query")
                .hidden(false);
        if (context.getOperationContext().getParameters().size() == 1 && TypeUtils.isComplexObjectType(erasedType)) {
            context.parameterBuilder().parameterType("body");
        }
        Optional<ApiParam> optional = readApiParam(context);
        if (optional.isPresent()) {
            ApiParam apiParam = optional.get();
            List<VendorExtension> extensions = buildExtensions(resolvedType);
            context.parameterBuilder().name(emptyToNull(apiParam.name()))
                    .description(emptyToNull(resolver.resolve(apiParam.value())))
                    .order(SWAGGER_PLUGIN_ORDER)
                    .parameterAccess(emptyToNull(apiParam.access()))
                    .defaultValue(emptyToNull(apiParam.defaultValue()))
                    .allowMultiple(apiParam.allowMultiple())
                    .allowEmptyValue(apiParam.allowEmptyValue())
                    .required(apiParam.required())
                    .scalarExample(new Example(apiParam.example()))
                    .complexExamples(examples(apiParam.examples()))
                    .collectionFormat(apiParam.collectionFormat())
                    .vendorExtensions(extensions);
        }
    }

    private String readName(ResolvedMethodParameter resolvedMethodParameter) {
        Optional<String> stringOptional = resolvedMethodParameter.defaultName();
        if (stringOptional.isPresent()) {
            return stringOptional.get();
        }
        return "arg" + resolvedMethodParameter.getParameterIndex();
    }

    private List<VendorExtension> buildExtensions(ResolvedType resolvedType) {
        List<VendorExtension> extensions = Lists.newArrayList();
        extensions.add(new StringVendorExtension("className", resolvedType.toString()));
        return extensions;
    }

    private Optional<ApiParam> readApiParam(ParameterContext context) {
        Optional<ApiMethod> optional = context.getOperationContext().findAnnotation(ApiMethod.class);
        if (optional.isPresent()) {
            ApiMethod apiMethod = optional.get();
            ResolvedMethodParameter parameter = context.resolvedMethodParameter();
            if (parameter.getParameterIndex() > apiMethod.params().length - 1) {
                throw new SwaggerMoreException("The number of parameters in method " + context.getOperationContext().getName() + " does not match the number of @ApiParam.");
            }
            return Optional.of(apiMethod.params()[parameter.getParameterIndex()]);
        }
        return Optional.absent();
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}
