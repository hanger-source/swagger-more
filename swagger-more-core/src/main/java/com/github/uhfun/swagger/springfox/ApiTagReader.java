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

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import org.springframework.util.StringUtils;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingBuilderPlugin;
import springfox.documentation.spi.service.contexts.ApiListingContext;

import static com.google.common.base.Optional.fromNullable;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author uhfun
 */
public class ApiTagReader implements ApiListingBuilderPlugin {
    @Override
    public void apply(ApiListingContext apiListingContext) {
        Optional<? extends Class<?>> controller = apiListingContext.getResourceGroup().getControllerClass();
        if (controller.isPresent()) {
            Optional<Api> apiAnnotation = fromNullable(findAnnotation(controller.get(), Api.class));
            if (apiAnnotation.isPresent()) {
                Api api = apiAnnotation.get();
                String tag;
                if (api.tags().length == 0 || StringUtils.isEmpty(tag = api.tags()[0])) {
                    tag = apiListingContext.getResourceGroup().getGroupName();
                }
                apiListingContext.apiListingBuilder()
                        .description(api.description())
                        .tagNames(Sets.newHashSet(tag));
            }
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}
