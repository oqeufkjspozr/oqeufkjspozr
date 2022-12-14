package com.alibaba.boot.dubbo;

import com.alibaba.boot.dubbo.endpoint.ConsumerBean;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.config.spring.AnnotationBean;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

public class DubboEndpoint extends AbstractEndpoint<List<Object>> implements ApplicationContextAware {

    private ApplicationContext context;

    public DubboEndpoint(){
        super("dubbo");
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public List<Object> invoke() {
        List<ProviderBean> publishedInterfaceList = new ArrayList<>();
        List<com.alibaba.boot.dubbo.endpoint.ConsumerBean> subscribedInterfaceList = new ArrayList<>();
        AnnotationBean annotationBean = context.getBean(AnnotationBean.class);
        Field serviceConfigsField = ReflectionUtils.findField(AnnotationBean.class, "serviceConfigs");
        ReflectionUtils.makeAccessible(serviceConfigsField);
        Object services = ReflectionUtils.getField(serviceConfigsField, annotationBean);
        if (services instanceof Set) {
            final Set<ServiceConfig<?>> serviceConfigs = (Set<ServiceConfig<?>>) services;
            for (ServiceConfig config : serviceConfigs) {
                ProviderBean providerBean = new ProviderBean();
                providerBean.setTarget(config.getStub());
                providerBean.setServiceInterface(config.getInterface());
                providerBean.setServiceVersion(config.getVersion());
                providerBean.setClientTimeout(config.getTimeout());
                providerBean.setMethodNames(config.getMethods());
                publishedInterfaceList.add(providerBean);
            }
        }
        Field referenceConfigsField = ReflectionUtils.findField(AnnotationBean.class, "referenceConfigs");
        ReflectionUtils.makeAccessible(referenceConfigsField);
        Object references = ReflectionUtils.getField(referenceConfigsField, annotationBean);
        if (references instanceof ConcurrentMap) {
            final ConcurrentMap<String, ReferenceBean<?>> referenceConfigs = (ConcurrentMap<String, ReferenceBean<?>>) references;
            for (Entry<String, ReferenceBean<?>> reference : referenceConfigs.entrySet()) {
                ReferenceBean referenceBean = reference.getValue();
                com.alibaba.boot.dubbo.endpoint.ConsumerBean consumerBean = new ConsumerBean();
                consumerBean.setGroup(consumerBean.getGroup());
                consumerBean.setInterfaceName(consumerBean.getInterfaceName());
                consumerBean.setMethodNames(consumerBean.getMethodNames());
                consumerBean.setVersion(consumerBean.getVersion());
                subscribedInterfaceList.add(consumerBean);
            }
        }
        List<Object> all = new ArrayList<Object>();
        Map<String, List> provider = new HashMap<String, List>();
        provider.put("provider", publishedInterfaceList);
        Map<String, List> consumer = new HashMap<String, List>();
        consumer.put("consumer", subscribedInterfaceList);
        all.add(provider);
        all.add(consumer);
        return all;
    }

}
