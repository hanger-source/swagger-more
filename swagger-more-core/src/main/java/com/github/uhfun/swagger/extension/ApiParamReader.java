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
package com.github.uhfun.swagger.extension;

import com.fasterxml.classmate.ResolvedType;
import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.common.SwaggerMoreException;
import com.github.uhfun.swagger.util.TypeUtils;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

import static com.github.uhfun.swagger.common.Constant.DEFAULT_COMPLEX_OBJECT_SUFFIX;
import static com.github.uhfun.swagger.common.Constant.GENERATED_PREFIX;
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

    @Autowired
    public ApiParamReader(DescriptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void apply(ParameterContext context) {
        ResolvedType resolvedType = context.resolvedMethodParameter().getParameterType();
        Class erasedType = resolvedType.getErasedType();
        if (isGeneratedType(erasedType)) {
            context.parameterBuilder()
                    .parameterType("body").name(erasedType.getSimpleName())
                    .description("Not a real parameter, it is a parameter generated after assembly.");
            return;
        }
        Optional<ApiParam> optional = readApiParam(context);
        if (optional.isPresent()) {
            ApiParam apiParam = optional.get();
            List<VendorExtension> extensions = buildExtensions(resolvedType);
            context.parameterBuilder().name(emptyToNull(apiParam.name()))
                    .description(emptyToNull(resolver.resolve(apiParam.value())))
                    .parameterType(TypeUtils.isComplexObjectType(erasedType) ? "body" : "query")
                    .order(SWAGGER_PLUGIN_ORDER)
                    .hidden(false)
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

    private List<VendorExtension> buildExtensions(ResolvedType resolvedType) {
        List<VendorExtension> extensions = Lists.newArrayList();
        extensions.add(new StringVendorExtension("className", resolvedType.toString()));
        return extensions;
    }

    private boolean isGeneratedType(Class type) {
        return type.getSimpleName().startsWith(GENERATED_PREFIX) && type.getSimpleName().endsWith(DEFAULT_COMPLEX_OBJECT_SUFFIX);
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
