package com.github.uhfun.swagger.more.dubbo.demo;

//import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;

import com.github.uhfun.swagger.annotaions.EnableSwaggerMore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * @author fuhangbo
 */
@SpringBootApplication
//@EnableDubboConfiguration
@EnableSwaggerMore
public class SwaggerDubboDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwaggerDubboDemoApplication.class, args);
    }
}
