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

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import com.google.common.collect.ImmutableMap;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.runtime.AbstractActor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ActorInfoContributorTest
{
    private ActorInfoContributorLifetimeExtension extension;

    @Before
    public void setUp() throws Exception
    {
        extension = new ActorInfoContributorLifetimeExtension(reference ->
        {
            // Ideally this would be a mock, but since Mockito keeps references in memory to arguments passed
            // to mocked methods, and some of these tests rely on actor instances being garbage collected, it's
            // necessary to create the fake object manually like so.
            Class<? extends AbstractActor> referenceClass = reference.getClass();
            if (FakeActorImpl.class.isAssignableFrom(referenceClass))
            {
                return FakeActor.class;
            }
            else if (OtherFakeActorImpl.class.isAssignableFrom(referenceClass))
            {
                return OtherFakeActor.class;
            }
            return null;
        });
    }

    private Map<String, Object> getInfo()
    {
        Info.Builder builder = new Info.Builder();
        extension.contribute(builder);
        return builder.build().getDetails();
    }

    @Test
    public void nothingRegistered_emptyMap() throws Exception
    {
        assertThat(getInfo(), equalTo(Collections.EMPTY_MAP));
    }

    @Test
    public void actorIsActive_getActorInfo_callsIntoActor() throws Exception
    {
        FakeActorImpl actor = spy(new FakeActorImpl("a", "b", "c"));
        extension.preActivation(actor).join();
        getInfo();
        verify(actor).contribute(any());
    }

    @Test
    public void twoActorsWithSameInterface_separatedByIdentity() throws Exception
    {
        FakeActorImpl fakeActorA = new FakeActorImpl("123", "baz", 8);
        FakeActorImpl fakeActorB = new FakeActorImpl("456", "biz", "goo");
        extension.preActivation(fakeActorA).join();
        extension.preActivation(fakeActorB).join();
        assertThat(getInfo().get("actors"), equalTo(ImmutableMap.of(
                "FakeActor", ImmutableMap.of(
                        "123", ImmutableMap.of(
                                "baz", 8),
                        "456", ImmutableMap.of(
                                "biz", "goo")))));
    }

    @Test
    public void twoActorsWithDifferentInterfaces_separatedByInterface() throws Exception
    {
        FakeActorImpl fakeActorA = new FakeActorImpl("123", "something", 100);
        OtherFakeActorImpl fakeActorB = new OtherFakeActorImpl("456", "number", 999);
        extension.preActivation(fakeActorA).join();
        extension.preActivation(fakeActorB).join();
        assertThat(getInfo().get("actors"), equalTo(ImmutableMap.of(
                "FakeActor", ImmutableMap.of(
                        "123", ImmutableMap.of(
                                "something", 100)),
                "OtherFakeActor", ImmutableMap.of(
                        "456", ImmutableMap.of(
                                "number", 999)))));
    }

    @Test
    public void actorGoesOffline_getsRemovedFromInfo() throws Exception
    {
        extension.preActivation(new FakeActorImpl("a", "b", "c")).join();
        System.gc();
        assertThat(getInfo(), equalTo(Collections.EMPTY_MAP));
    }

    @Test
    public void testThreadSafe() throws Exception
    {
        Exception[] exceptionFromThread = new Exception[1];
        Runnable runnable = () ->
        {
            List<Integer> indices = IntStream.range(0, 30).boxed().collect(Collectors.toList());
            Collections.shuffle(indices);
            String[] randomStrings = new String[]{"foo", "bar", "fiz", "buz", "panama"};
            Random r = new Random();
            try
            {
                for (int i : indices)
                {
                    if (r.nextBoolean())
                    {
                        extension.preActivation(new FakeActorImpl(String.valueOf(i),
                                randomStrings[r.nextInt(randomStrings.length)],
                                randomStrings[r.nextInt(randomStrings.length)])).join();
                    }
                    else
                    {
                        getInfo();
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

    @Test
    public void actorDoesNotImplementInfoContributor_actorDoesNotGetProcessed() throws Exception
    {
        NonInfoContributorFakeActor actor = new NonInfoContributorFakeActor();
        extension.preActivation(actor).join();
        assertThat(getInfo(), equalTo(Collections.EMPTY_MAP));
    }

    private interface FakeActor extends Actor
    {
    }

    private static class FakeActorImpl extends AbstractActor implements FakeActor, InfoContributor
    {
        private final String identity;
        private final String key;
        private final Object value;

        private FakeActorImpl(final String identity, final String key, final Object value)
        {
            this.identity = identity;
            this.key = key;
            this.value = value;
        }

        @Override
        public String getIdentity()
        {
            return identity;
        }

        @Override
        public void contribute(final Info.Builder builder)
        {
            builder.withDetail(key, value);
        }
    }

    private static class NonInfoContributorFakeActor extends AbstractActor implements FakeActor
    {
    }

    private interface OtherFakeActor extends Actor
    {
    }

    private static class OtherFakeActorImpl extends AbstractActor implements OtherFakeActor, InfoContributor
    {
        private final String identity;
        private final String key;
        private final Object value;

        private OtherFakeActorImpl(final String identity, final String key, final Object value)
        {
            this.identity = identity;
            this.key = key;
            this.value = value;
        }

        @Override
        public String getIdentity()
        {
            return identity;
        }

        @Override
        public void contribute(final Info.Builder builder)
        {
            builder.withDetail(key, value);
        }
    }
}
