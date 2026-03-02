package com.example.gamestore.lifecycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Bean used to demonstrate the full Spring lifecycle:
 * Constructor -> Aware interfaces -> @PostConstruct -> afterPropertiesSet ->
 * (bean in use) -> @PreDestroy -> destroy.
 */
@Component
public class LifecycleConsole implements BeanNameAware,
                                         ApplicationContextAware,
                                         InitializingBean,
                                         DisposableBean {

    public LifecycleConsole() {
        System.out.println("1. Constructor - bean instantiated");
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("2. BeanNameAware.setBeanName - name = " + name);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        System.out.println("3. ApplicationContextAware.setApplicationContext - context set");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("4. @PostConstruct - initialization callback");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("5. InitializingBean.afterPropertiesSet - all properties set");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("6. @PreDestroy - cleanup before bean destruction");
    }

    @Override
    public void destroy() {
        System.out.println("7. DisposableBean.destroy - final cleanup");
    }
}

