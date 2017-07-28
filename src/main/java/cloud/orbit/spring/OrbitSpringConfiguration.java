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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.cloner.ExecutionObjectCloner;
import cloud.orbit.actors.cluster.ClusterPeer;
import cloud.orbit.actors.extensions.ActorConstructionExtension;
import cloud.orbit.actors.extensions.ActorExtension;
import cloud.orbit.actors.extensions.MessageSerializer;
import cloud.orbit.actors.runtime.*;
import cloud.orbit.concurrent.ExecutorUtils;

import java.time.Clock;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(OrbitActorsProperties.class)
public class OrbitSpringConfiguration
{
    @Autowired(required = false)
    private List<OrbitSpringConfigurationAddon> configAddons;

    @Autowired(required = false)
    @Qualifier("stageClock")
    private Clock stageClock;

    @Autowired(required = false)
    private Execution execution;

    @Autowired(required = false)
    private LocalObjectsCleaner localObjectsCleaner;

    @Autowired(required = false)
    private ClusterPeer clusterPeer;

    @Autowired(required = false)
    @Qualifier("executionObjectCloner")
    private ExecutionObjectCloner executionObjectCloner;

    @Autowired(required = false)
    private MessageSerializer messageSerializer;

    @Autowired(required = false)
    @Qualifier("messageLoopbackObjectCloner")
    private ExecutionObjectCloner messageLoopbackObjectCloner;

    @Autowired(required = false)
    private InvocationHandler invocationHandler;

    @Autowired(required = false)
    @Qualifier("stageTimer")
    private Timer stageTimer;

    @Bean
    @ConditionalOnMissingBean(ActorConstructionExtension.class)
    public ActorConstructionExtension springActorConstructionExtension(AutowireCapableBeanFactory factory)
    {
        return new SpringActorConstructionExtension(factory);
    }

    @Bean
    @ConditionalOnMissingBean(Messaging.class)
    public Messaging messaging(OrbitActorsProperties properties)
    {
        Messaging messaging = new Messaging();
        if (properties.getMessagingTimeoutInMilliseconds() != null)
        {
            messaging.setResponseTimeoutMillis(properties.getMessagingTimeoutInMilliseconds());
        }
        return messaging;
    }

    @Bean
    @ConditionalOnMissingBean(name = "stageExecutorService")
    public ExecutorService stageExecutorService(OrbitActorsProperties properties)
    {
        // same as default execution pool from Orbit's Stage class
        int poolSize = properties.getExecutionPoolSize() != null ? properties.getExecutionPoolSize() : 128;
        return ExecutorUtils.newScalingThreadPool(poolSize);
    }

    @Bean
    @ConditionalOnMissingBean(Stage.class)
    public Stage stage(OrbitActorsProperties properties,
                       List<ActorExtension> actorExtensions,
                       Messaging messaging,
                       @Qualifier("stageExecutorService") ExecutorService stageExecutorService)
    {
        Stage stage = buildStage(properties, actorExtensions, messaging, stageExecutorService);

        if (configAddons != null)
        {
            configAddons.forEach(addon -> addon.configure(stage));
        }

        stage.start().join();
        stage.bind();

        return stage;
    }

    Stage buildStage(final OrbitActorsProperties properties,
                     final List<ActorExtension> actorExtensions,
                     final Messaging messaging,
                     final ExecutorService stageExecutorService)
    {
        Assert.notNull(properties);

        Stage.Builder stageBuilder = new Stage.Builder()
                .clock(stageClock)
                .executionPool(stageExecutorService)
                .execution(execution)
                .localObjectsCleaner(localObjectsCleaner)
                .clusterPeer(clusterPeer)
                .objectCloner(executionObjectCloner)
                .messageSerializer(messageSerializer)
                .messageLoopbackObjectCloner(messageLoopbackObjectCloner)
                .messaging(messaging)
                .invocationHandler(invocationHandler)
                .timer(stageTimer)
                .clusterName(properties.getClusterName())
                .nodeName(properties.getNodeName());

        if (actorExtensions != null)
        {
            stageBuilder.extensions(actorExtensions.toArray(new ActorExtension[actorExtensions.size()]));
        }

        if (properties.getBasePackages() != null)
        {
            stageBuilder.basePackages(properties.getBasePackages());
        }

        if (properties.getStageMode() != null)
        {
            stageBuilder.mode(properties.getStageMode());
        }

        if (properties.getTimeToLiveInSeconds() != null)
        {
            stageBuilder.actorTTL(properties.getTimeToLiveInSeconds(), TimeUnit.SECONDS);
        }

        if (properties.getStickyHeaders() != null)
        {
            properties.getStickyHeaders().forEach(stageBuilder::stickyHeaders);
        }

        if (properties.getConcurrentDeactivations() != null)
        {
            stageBuilder.concurrentDeactivations(properties.getConcurrentDeactivations());
        }

        if (properties.getDeactivationTimeoutInMilliseconds() != null)
        {
            stageBuilder.deactivationTimeout(properties.getDeactivationTimeoutInMilliseconds(), TimeUnit.MILLISECONDS);
        }

        if (properties.getExecutionPoolSize() != null)
        {
            stageBuilder.executionPoolSize(properties.getExecutionPoolSize());
        }

        if(properties.getLocalAddressCacheTTLInMilliseconds() != null)
        {
            stageBuilder.localAddressCacheTTL(properties.getLocalAddressCacheTTLInMilliseconds(), TimeUnit.MILLISECONDS);
        }

        if(properties.getLocalAddressCacheMaximumSize() != null)
        {
            stageBuilder.localAddressCacheMaximumSize(properties.getLocalAddressCacheMaximumSize());
        }

        if(properties.getBroadcastActorDeactivations() != null)
        {
            stageBuilder.broadcastActorDeactivations(properties.getBroadcastActorDeactivations());
        }

        return stageBuilder.build();
    }
}
