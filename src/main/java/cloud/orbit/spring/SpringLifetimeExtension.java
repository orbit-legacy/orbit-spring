/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.
 */

package cloud.orbit.spring;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

class SpringLifetimeExtension implements LifetimeExtension {
    private final AutowireCapableBeanFactory beanFactory;

    public SpringLifetimeExtension(AutowireCapableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    // Provides constructor injection for actors
    @Override
    public <T> T newInstance(Class<T> concreteClass) {
        T instance = (T)beanFactory.createBean(concreteClass);
        return instance;
    }

    // Provides property injection for actors
    @Override
    public Task preActivation(AbstractActor actor) {
        beanFactory.autowireBeanProperties(actor, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE ,false);
        return Task.done();
    }
}
