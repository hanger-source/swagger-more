package com.github.uhfun.swagger.more.dubbo.demo.api;


import com.github.uhfun.swagger.annotations.ApiMethod;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

/**
 * 演示服务(根据javadoc 生成注解, 运行前执行 mvn package)
 * @author uhfun
 */
@Api
public interface DemoDubboService {
    /**
     * 测试(根据javadoc 生成注解)
     * @param id 唯一键
     * @param demo demo1
     */
    Demo test(String id, Demo demo);

    @ApiMethod(value = "测试", params = {
            @ApiParam(name = "id", value = "唯一键"),
            @ApiParam(name = "demo", value = "demo2"),
    })
    Demo test2(String id, Demo demo);
}
