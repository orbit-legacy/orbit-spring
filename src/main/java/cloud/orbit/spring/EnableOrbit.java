/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.
 */

package cloud.orbit.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * This attribute will enable the Orbit stage bean and create a default stage
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({OrbitBeanDefinitionRegistrar.class})
public @interface EnableOrbit {
    String[] value() default {};
}
