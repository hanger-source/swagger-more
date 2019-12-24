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
package com.github.uhfun.swagger.dubbo;

/**
 * @author uhfun
 */
public class DefaultServiceBean implements ServiceBean {
    private Class interfaceClass;
    private Object ref;

    DefaultServiceBean(Class interfaceClass, Object ref) {
        this.interfaceClass = interfaceClass;
        this.ref = ref;
    }

    @Override
    public Class getInterfaceClass() {
        return interfaceClass;
    }

    @Override
    public Object getRef() {
        return ref;
    }
}
