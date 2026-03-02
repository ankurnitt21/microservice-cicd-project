package com.example.gamestore.lifecycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * BeanFactoryPostProcessor that tweaks the BeanDefinition of gameSession
 * before any instances are created (operates on blueprints, not objects).
 */
@Component
public class GameSessionBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory.containsBeanDefinition("gameSession")) {
            BeanDefinition bd = beanFactory.getBeanDefinition("gameSession");
            // For demonstration we ensure it is prototype (it already is in code).
            bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            System.out.println("BFPP: Modified BeanDefinition for 'gameSession' (scope set to prototype).");
        }
    }
}

