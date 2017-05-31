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

package cloud.orbit.spring.actuate;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.LocalManagementPort;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableMap;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.NoIdentity;
import cloud.orbit.actors.annotation.StatelessWorker;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;
import cloud.orbit.spring.OrbitBeanDefinitionRegistrar;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = OrbitBeanDefinitionRegistrar.class)
@EnableAutoConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ActorInfoContributorIntegrationTest
{
    @SuppressWarnings("unused")
    @LocalManagementPort
    private int port;

    @Autowired
    private Stage stage;

    @Autowired
    private Executor executor;

    @Autowired
    private ActorInfoContributorLifetimeExtension actorInfoContributorLifetimeExtension;

    private Map<String, Object> result;

    @After
    public void tearDown() throws Exception
    {
        // The @DirtiesContext annotation doesn't take care of the stage, so we do it explicitly
        stage.stop().join();
    }

    @Test
    public void usesDefaultOrbitThreadPool() throws Exception
    {
        assertThat(executor, instanceOf(ForkJoinPool.class));
    }

    @Test
    public void noActorInteraction_noInfo() throws Exception
    {
        readInfoEndpoint();
        assertThat(result, equalTo(Collections.EMPTY_MAP));
    }

    @Test
    public void activateBasicActor_hasInfo() throws Exception
    {
        Actor.getReference(ActorWithInjection.class, "0").setStat(10).join();
        readInfoEndpoint();
        assertThat(result.get("actors"), equalTo(ImmutableMap.of(
                "ActorWithInjection", ImmutableMap.of(
                        "0", ImmutableMap.of(
                                "stat", 10,
                                "constructorInjectedValue", 99,
                                "fieldInjectedValue", 200)))));
    }

    @Test
    public void activateActorWithNoIdentity_hasInfo() throws Exception
    {
        Actor.getReference(ActorNoIdentity.class).setStat(-10).join();
        readInfoEndpoint();
        assertThat(result.get("actors"), equalTo(ImmutableMap.of(
                "ActorNoIdentity", ImmutableMap.of(
                        "null", ImmutableMap.of(
                                "stat", -10)))));
    }

    @Test
    public void activateTwoActorsWithNoIdentity_hasInfoForLatestOneOnly() throws Exception
    {
        Actor.getReference(ActorNoIdentity.class).setStat(-10).join();
        Actor.getReference(ActorNoIdentity.class).setStat(72).join();
        readInfoEndpoint();
        assertThat(result.get("actors"), equalTo(ImmutableMap.of(
                "ActorNoIdentity", ImmutableMap.of(
                        "null", ImmutableMap.of(
                                "stat", 72)))));
    }

    @Test
    public void activateStatelessWorker_hasInfo() throws Exception
    {
        Actor.getReference(FakeStatelessWorker.class, "foo").setStat(2).join();
        readInfoEndpoint();
        assertThat(result.get("actors"), equalTo(ImmutableMap.of(
                "FakeStatelessWorker", ImmutableMap.of(
                        "foo", ImmutableMap.of(
                                "stat", 2)))));
    }

    @Test
    public void activateManyActorsOfDifferentKinds_hasInfoForEverything() throws Exception
    {
        Actor.getReference(ActorWithInjection.class, "foo").setStat(2315).join();
        Actor.getReference(ActorWithInjection.class, "bar").setStat(89).join();
        Actor.getReference(ActorNoIdentity.class).setStat(99).join();
        Actor.getReference(FakeStatelessWorker.class, "12").setStat(2).join();
        Actor.getReference(FakeStatelessWorker.class, "biz").setStat(-14).join();
        readInfoEndpoint();
        assertThat(result.get("actors"), equalTo(ImmutableMap.of(
                "ActorWithInjection", ImmutableMap.of(
                        "foo", ImmutableMap.of(
                                "stat", 2315,
                                "constructorInjectedValue", 99,
                                "fieldInjectedValue", 200),
                        "bar", ImmutableMap.of(
                                "stat", 89,
                                "constructorInjectedValue", 99,
                                "fieldInjectedValue", 200)
                ),
                "ActorNoIdentity", ImmutableMap.of(
                        "null", ImmutableMap.of(
                                "stat", 99)
                ),
                "FakeStatelessWorker", ImmutableMap.of(
                        "12", ImmutableMap.of(
                                "stat", 2),
                        "biz", ImmutableMap.of(
                                "stat", -14)
                ))));
    }

    @Test
    public void actorActivationTriggersAnotherActorActivation_noThreadLockAndBothContributeActorInfo() throws Exception
    {
        Actor.getReference(ActorWithCustomActivation.class, "jerry").setStat(101).join();
        readInfoEndpoint();
        assertThat(result.get("actors"), equalTo(ImmutableMap.of(
                "ActorWithInjection", ImmutableMap.of(
                        "indiana", ImmutableMap.of(
                                "stat", 75,
                                "constructorInjectedValue", 99,
                                "fieldInjectedValue", 200)
                ),
                "ActorWithCustomActivation", ImmutableMap.of(
                        "jerry", ImmutableMap.of(
                                "stat", 101)
                ))));
    }

    @Test
    public void testThreadSafe() throws Exception
    {
        Exception[] exceptionFromThread = new Exception[1];
        Runnable runnable = () ->
        {
            List<Integer> indices = IntStream.range(0, 30).boxed().collect(Collectors.toList());
            Collections.shuffle(indices);
            Random r = new Random();
            try
            {
                for (int i : indices)
                {
                    if (r.nextBoolean())
                    {
                        Actor.getReference(ActorWithInjection.class, String.valueOf(i)).touch().join();
                    }
                    else
                    {
                        actorInfoContributorLifetimeExtension.contribute(new Info.Builder());
                    }
                }
            }
            catch (Exception e)
            {
                exceptionFromThread[0] = e;
            }
        };
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++)
        {
            threads[i] = new Thread(runnable);
        }
        for (final Thread thread : threads)
        {
            thread.start();
        }
        for (final Thread thread : threads)
        {
            thread.join();
        }
        assertNull(exceptionFromThread[0]);
    }

    private void readInfoEndpoint() throws InterruptedException
    {
        waitForBackgroundProcessToComplete();
        //noinspection unchecked
        result = new RestTemplate().getForObject(String.format("http://localhost:%d/info", port), LinkedHashMap.class);
    }

    private void waitForBackgroundProcessToComplete() throws InterruptedException
    {
        while (((ForkJoinPool) executor).getActiveThreadCount() > 0)
        {
            Thread.sleep(1);
        }
    }

    @TestConfiguration
    public static class ConfigurationUsedToVerifyInjectionStillWorks
    {
        @Bean
        public int constructorInjectedValue()
        {
            return 99;
        }

        @Bean
        public int fieldInjectedValue()
        {
            return 200;
        }
    }

    private interface ActorWithStat
    {
        Task<Void> setStat(int stat);
    }

    private static class ActorWithStatImpl extends AbstractActor implements ActorWithStat, InfoContributor
    {
        private int stat;

        @Override
        public Task<Void> setStat(final int stat)
        {
            this.stat = stat;
            return Task.done();
        }

        @Override
        public void contribute(final Info.Builder builder)
        {
            builder.withDetail("stat", this.stat);
        }
    }

    public interface ActorWithInjection extends ActorWithStat, Actor
    {
        Task<Void> touch();
    }

    public static class ActorWithInjectionImpl extends ActorWithStatImpl implements ActorWithInjection
    {
        private final int constructorInjectedValue;

        @SuppressWarnings("SpringJavaAutowiredMembersInspection")
        @Autowired
        private int fieldInjectedValue;

        public ActorWithInjectionImpl(final int constructorInjectedValue)
        {
            this.constructorInjectedValue = constructorInjectedValue;
        }

        @Override
        public void contribute(final Info.Builder builder)
        {
            builder.withDetail("constructorInjectedValue", constructorInjectedValue)
                    .withDetail("fieldInjectedValue", fieldInjectedValue);
            super.contribute(builder);
        }

        @Override
        public Task<Void> touch()
        {
            return Task.done();
        }
    }

    @NoIdentity
    public interface ActorNoIdentity extends ActorWithStat, Actor
    {
    }

    public static class ActorNoIdentityImpl extends ActorWithStatImpl implements ActorNoIdentity
    {
    }

    @StatelessWorker
    public interface FakeStatelessWorker extends ActorWithStat, Actor
    {
    }

    public static class FakeStatelessWorkerImpl extends ActorWithStatImpl implements FakeStatelessWorker
    {
    }

    public interface ActorWithCustomActivation extends ActorWithStat, Actor
    {
    }

    public static class ActorWithCustomActivationImpl extends ActorWithStatImpl implements ActorWithCustomActivation
    {
        @Override
        public Task<?> activateAsync()
        {
            // activate an actor that implements InfoContributor to test that we don't thread lock
            Actor.getReference(ActorWithInjection.class, "indiana").setStat(75).join();
            return super.activateAsync();
        }
    }
}
