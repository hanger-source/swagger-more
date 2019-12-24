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

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.AbstractList;
import java.util.Comparator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

/**
 * 兼容 alibaba dubbo 和 apache dubbo
 *
 * @author uhfun
 */
@Slf4j
@Component
public class ServiceBeans extends AbstractList<ServiceBean> implements InitializingBean, ApplicationContextAware {
    private List<ServiceBean> serviceBeanList;
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() {
        serviceBeanList = Lists.newArrayList();
        loadAlibabaDuboServiceBean();
        loadApacheDuboServiceBean();
    }

    private void loadAlibabaDuboServiceBean() {
        try {
            Class alibabaDubboClass = ClassUtils.forName("com.alibaba.dubbo.config.spring.ServiceBean", ClassUtils.getDefaultClassLoader());
            for (Object bean : applicationContext.getBeansOfType(alibabaDubboClass).values()) {
                serviceBeanList.add(new AlibabaDubboServiceBean((com.alibaba.dubbo.config.spring.ServiceBean) bean));
            }
            if (serviceBeanList.isEmpty()) {
                Class<?> serviceClazz = ClassUtils.forName("com.alibaba.dubbo.config.annotation.Service", ClassUtils.getDefaultClassLoader());
                for (Object object : applicationContext.getBeansWithAnnotation((Class<? extends com.alibaba.dubbo.config.annotation.Service>) serviceClazz).values()) {
                    Class interfaceClass = object.getClass().getAnnotation(com.alibaba.dubbo.config.annotation.Service.class).interfaceClass();
                    if (void.class.equals(interfaceClass) && object.getClass().getInterfaces().length > 0) {
                        interfaceClass = object.getClass().getInterfaces()[0];
                    }
                    if (nonNull(interfaceClass)) {
                        serviceBeanList.add(new DefaultServiceBean(interfaceClass, object));
                    }
                }
            }

        } catch (ClassNotFoundException | BeansException e) {
            log.warn(e.getMessage());
        }
    }

    private void loadApacheDuboServiceBean() {
        try {
            Class alibabaDubboClass = ClassUtils.forName("org.apache.dubbo.config.spring.ServiceBean", ClassUtils.getDefaultClassLoader());
            for (Object bean : applicationContext.getBeansOfType(alibabaDubboClass).values()) {
                serviceBeanList.add(new ApacheDubboServiceBean((org.apache.dubbo.config.spring.ServiceBean) bean));
            }
            if (serviceBeanList.isEmpty()) {
                Class<?> serviceClazz = ClassUtils.forName("org.apache.dubbo.config.annotation.Service", ClassUtils.getDefaultClassLoader());
                for (Object object : applicationContext.getBeansWithAnnotation((Class<? extends org.apache.dubbo.config.annotation.Service>) serviceClazz).values()) {
                    Class interfaceClass = object.getClass().getAnnotation(org.apache.dubbo.config.annotation.Service.class).interfaceClass();
                    if (void.class.equals(interfaceClass) && object.getClass().getInterfaces().length > 0) {
                        interfaceClass = object.getClass().getInterfaces()[0];
                    }
                    if (nonNull(interfaceClass)) {
                        serviceBeanList.add(new DefaultServiceBean(interfaceClass, object));
                    }
                }
            }
        } catch (ClassNotFoundException | BeansException e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Stream<ServiceBean> stream() {
        return serviceBeanList.stream();
    }

    @Override
    public Stream<ServiceBean> parallelStream() {
        return serviceBeanList.stream();
    }


    @Override
    public ServiceBean get(int index) {
        return serviceBeanList.get(index);
    }

    @Override
    public void forEach(Consumer<? super ServiceBean> action) {
        serviceBeanList.forEach(action);
    }

    @Override
    public Spliterator<ServiceBean> spliterator() {
        return serviceBeanList.spliterator();
    }

    @Override
    public int size() {
        return serviceBeanList.size();
    }

    @Override
    public boolean removeIf(Predicate<? super ServiceBean> filter) {
        return serviceBeanList.removeIf(filter);
    }

    @Override
    public void replaceAll(UnaryOperator<ServiceBean> operator) {
        serviceBeanList.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super ServiceBean> c) {
        serviceBeanList.sort(c);
    }
}
