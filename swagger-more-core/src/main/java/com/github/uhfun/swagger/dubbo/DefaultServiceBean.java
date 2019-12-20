package com.github.uhfun.swagger.dubbo;

public class DefaultServiceBean implements ServiceBean {
    private Class interfaceClass;
    private Object ref;

    public DefaultServiceBean(Class interfaceClass, Object ref) {
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
