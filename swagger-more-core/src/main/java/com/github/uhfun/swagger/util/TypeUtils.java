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

package com.github.uhfun.swagger.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static springfox.documentation.schema.Types.typeNameFor;

/**
 * @author uhfun
 */
public class TypeUtils {
    public static boolean isContainerType(Class type) {
        return List.class.isAssignableFrom(type) ||
                Set.class.isAssignableFrom(type) ||
                (Collection.class.isAssignableFrom(type) && !isMapType(type)) ||
                type.isArray();
    }

    public static boolean isBaseType(Class type) {
        return springfox.documentation.schema.Types.isBaseType(typeNameFor(type));
    }

    public static boolean isMapType(Class type) {
        return Map.class.isAssignableFrom(type);
    }

    public static boolean isComplexObjectType(Class<?> type) {
        return !isContainerType(type) && !isBaseType(type) && !isMapType(type);
    }

}
