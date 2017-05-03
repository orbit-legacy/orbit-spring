/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.
 */

package cloud.orbit.spring;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.Messaging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(OrbitActorsProperties.class)
public class OrbitSpringConfiguration {
    @Autowired
    AutowireCapableBeanFactory factory;

    @Autowired(required = false)
    List<OrbitSpringConfigurationAddon> configAddons;

    @Bean
    public Stage stage(OrbitActorsProperties properties) {
        Stage.Builder stageBuilder = new Stage.Builder()
            .extensions(new SpringLifetimeExtension(factory));

        if (properties.getClusterName() != null) {
            stageBuilder.clusterName(properties.getClusterName());
        }

        if (properties.getNodeName() != null) {
            stageBuilder.nodeName(properties.getNodeName());
        }

        if (properties.getStageMode() != null)
        {
            stageBuilder.mode(properties.getStageMode());
        }

        if (properties.getTimeToLiveInSeconds() != null) {
            stageBuilder.actorTTL(properties.getTimeToLiveInSeconds(), TimeUnit.SECONDS);
        }

        if (properties.getMessagingTimeoutInMilliseconds() != null) {
            Messaging orbitMessaging = new Messaging();
            orbitMessaging.setResponseTimeoutMillis(properties.getMessagingTimeoutInMilliseconds());
            stageBuilder.messaging(orbitMessaging);
        }

        if (properties.getStickyHeaders() != null) {
            properties.getStickyHeaders().forEach(stageBuilder::stickyHeaders);
        }

        Stage stage = stageBuilder.build();

        if (configAddons != null) {
            configAddons.forEach(addon -> addon.configure(stage));
        }

        stage.start().join();
        stage.bind();

        return stage;
    }
}
