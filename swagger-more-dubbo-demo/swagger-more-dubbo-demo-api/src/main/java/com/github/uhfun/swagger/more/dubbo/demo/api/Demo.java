package com.github.uhfun.swagger.more.dubbo.demo.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author fuhangbo
 */
@ApiModel(description = "演示对象")
@Data
public class Demo implements Serializable {
    private static final long serialVersionUID = 9078795577535261073L;
    @ApiModelProperty(value = "Demo唯一键", name = "id")
    private String id;
    @ApiModelProperty(value = "名称", name = "name")
    private String name;
}
