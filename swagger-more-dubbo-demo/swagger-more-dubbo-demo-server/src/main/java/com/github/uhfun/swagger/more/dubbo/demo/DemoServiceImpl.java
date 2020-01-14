package com.github.uhfun.swagger.more.dubbo.demo;

import com.github.uhfun.swagger.more.dubbo.demo.api.Demo;
import com.github.uhfun.swagger.more.dubbo.demo.api.DemoDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.stereotype.Component;

/**
 * @author uhfun
 */
@Service(interfaceClass = DemoDubboService.class)
@Component("demoDubboService")
@Slf4j
public class DemoServiceImpl implements DemoDubboService {

    @Override
    public Demo test(String id, Demo demo) {
        String get = "已调用";
        demo.setName(demo.getName() + get);
        demo.setId(demo.getId() + get + ", id = " + id);
        return demo;
    }

    @Override
    public Demo test2(String id, Demo demo) {
        return test(id, demo);
    }
}
