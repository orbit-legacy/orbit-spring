/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cloud.orbit.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.extensions.ActorExtension;
import cloud.orbit.actors.runtime.Messaging;

import java.util.Arrays;
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
        Stage.Builder stageBuilder = new Stage.Builder();

        if (properties.getBasePackages() != null) {
            stageBuilder.basePackages(properties.getBasePackages());
        }

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

        List<ActorExtension> actorExtensions = Arrays.asList(new SpringLifetimeExtension(factory));
        if (properties.getExtensions() != null) {
            actorExtensions.addAll(properties.getExtensions());
        }
        stageBuilder.extensions(actorExtensions.toArray(new ActorExtension[actorExtensions.size()]));

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

        if (properties.getConcurrentDeactivations() != null) {
            stageBuilder.concurrentDeactivations(properties.getConcurrentDeactivations());
        }

        if (properties.getDeactivationTimeoutMillis() != null) {
            stageBuilder.deactivationTimeout(properties.getDeactivationTimeoutMillis(), TimeUnit.MILLISECONDS);
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
