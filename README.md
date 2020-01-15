# swagger-more
[![Build Status](https://travis-ci.org/uhfun/swagger-more.svg?branch=master)](https://travis-ci.org/uhfun/swagger-more) ![jdk1.8](https://img.shields.io/badge/jdk-1.8-blue.svg) 

## About

​		公司内部测试需要dubbo接口提供文档， 实习的闲暇之余有好奇Swagger2的代码，看看它怎么大致的流程如何，后面又希望能灵活调试 。由此想到基于springfox swagger2来实现一个类似swagger http文档的dubbo文档。  

​        github上也有类似功能的项目, 不过项目存在问题并且作者也没有维护

* https://github.com/Sayi/swagger-dubbo

* https://github.com/zhaojigang/springfox

  动态生成带注解的Controller来进行api信息的读取，但是个人感觉这个做法不是特别优雅，springfox官方已经提供了一套扩展性比较强的接口来可以实现api信息的读取。



## UI

![UI](https://raw.githubusercontent.com/uhfun/swagger-more/master/ui.png)

> **依旧支持官方的swagger ui**
>
> UI页面支持
>
>  https://github.com/shuangbofu/swagger-more-portal

## Features

1. 自动生成dubbo接口文档，同springfox 的JSON API
2. 接口支持调试，支持泛型调用，解决接口方法多对象传参以及方法重载的问题
3. 支持javadoc生成接口信息的注解
4. 兼容springfox swagger2的ui、提供一个符合java接口文档的新UI
5. 兼容alibaba dubbo 和 apache dubbo


## Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>com.github.uhfun</groupId>
        <artifactId>swagger-more-annotations</artifactId>
        <version>1.0.2-SNAPSHOT</version>
    </dependency>
    <dependency>
         <groupId>com.github.uhfun</groupId>
        <artifactId>swagger-more-core</artifactId>
        <version>1.0.2-SNAPSHOT</version>
    </dependency>
</dependencies>
```


## How to use

[移步wiki](https://github.com/uhfun/swagger-more/wiki/How-to-use)

## Versions

- [1.0.2-SNAPSHOT](https://github.com/uhfun/swagger-more/tree/1.0.2-SNAPSHOT)
    1. 「 controller查找方法后调用 」改为「 根据dubbo service 生成 RequestMapping 」    
    2. 「 controller参数解析 」改为 「 自定义HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler 」    
    3. 「 controller日志打印 」改为「 @service 注解切面日志 」    
    4.  新增代理方法转换参数    

- [1.0.1-SNAPSHOT](https://github.com/uhfun/swagger-more/tree/1.0.1-SNAPSHOT)
    1. 修复tag为空
    2. 兼容alibaba dubbo 和 apache dubbo
    3. javadoc title为空异常
    4. 添加demo

- 1.0.0
    1. 自动生成dubbo接口文档，同springfox 的JSON API
    2. 接口支持调试，支持泛型调用，解决接口方法多对象传参以及方法重载的问题
    3. 支持javadoc生成接口信息的注解
    4. 兼容springfox swagger2的ui、提供一个符合java接口文档的新UI


## Copyright

```
Copyright (c) 2019 uhfun

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```