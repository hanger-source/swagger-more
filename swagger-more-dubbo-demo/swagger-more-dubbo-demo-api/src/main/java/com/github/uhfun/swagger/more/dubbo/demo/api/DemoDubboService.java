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
     * 测试(根据javadoc 生成注解, 运行前执行 mvn package)
     * @param id 唯一键
     * @param demo 演示对象参数
     * @return demo
     */
    Demo test(String id, Demo demo);

    @ApiMethod(value = "测试", params = {
            @ApiParam(name = "id", value = "唯一键"),
            @ApiParam(name = "name", value = "名称"),
    })
    void test2(String id, String name);

    @ApiMethod(value = "不展示的方法", hidden = true)
    void hiddenMethod();

    /**
     * 不展示的方法Javadoc
     *
     * @hidden
     */
    void hiddenMethodJavadoc();
}
