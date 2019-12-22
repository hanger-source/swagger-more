package com.github.uhfun.swagger.extension;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingBuilderPlugin;
import springfox.documentation.spi.service.contexts.ApiListingContext;

import static com.google.common.base.Optional.fromNullable;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author fuhangbo
 */
@Component
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
