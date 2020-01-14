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
import com.fasterxml.classmate.TypeResolver;
import com.github.uhfun.swagger.annotations.ApiMethod;
import com.github.uhfun.swagger.util.TypeUtils;
import com.google.common.base.Optional;
import io.swagger.annotations.ApiModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationModelsProviderPlugin;
import springfox.documentation.spi.service.contexts.OperationModelContextsBuilder;
import springfox.documentation.spi.service.contexts.RequestMappingContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author uhfun
 */
public class ApiMethodModelsProvider implements OperationModelsProviderPlugin {

    private final TypeResolver typeResolver;

    @Autowired
    public ApiMethodModelsProvider(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void apply(RequestMappingContext context) {
        collectFromReturnType(context);
        collectApiMethodParams(context);
    }

    private void collectFromReturnType(RequestMappingContext context) {
        ResolvedType modelType = context.alternateFor(context.getReturnType());
        context.operationModelsBuilder().addReturn(modelType);
    }

    private void collectApiMethodParams(RequestMappingContext context) {
        List<ResolvedMethodParameter> parameterTypes = context.getParameters();
        Optional<ApiMethod> optional = context.findAnnotation(ApiMethod.class);
        OperationModelContextsBuilder builder = context.operationModelsBuilder();
        if (optional.isPresent()) {
            parameterTypes.forEach(parameter -> collectAllTypes(context, parameter).forEach(builder::addInputParam));
        }
    }

    private List<ResolvedType> collectAllTypes(RequestMappingContext context, ResolvedMethodParameter parameter) {
        List<ResolvedType> allTypes = newArrayList();
        for (ResolvedType type : collectBindingTypes(context.alternateFor(parameter.getParameterType()), newArrayList())) {
            ApiModel apiModel = AnnotationUtils.getAnnotation(type.getErasedType(), ApiModel.class);
            allTypes.add(type);
            if (apiModel != null) {
                allTypes.addAll(Arrays.stream(apiModel.subTypes())
                        .filter(subType -> subType.getAnnotation(ApiModel.class) != type.getErasedType().getAnnotation(ApiModel.class))
                        .map(typeResolver::resolve).collect(Collectors.toList()));
            }
        }
        return allTypes;
    }

    private List<ResolvedType> collectBindingTypes(ResolvedType type, List<ResolvedType> types) {
        if (TypeUtils.isComplexObjectType(type.getErasedType())) {
            types.add(type);
        }
        if (TypeUtils.isBaseType(type.getErasedType())
                || type.getTypeBindings().isEmpty()) {
            return types;
        }
        for (ResolvedType resolvedType : type.getTypeBindings().getTypeParameters()) {
            collectBindingTypes(resolvedType, types);
        }
        return types;
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}
