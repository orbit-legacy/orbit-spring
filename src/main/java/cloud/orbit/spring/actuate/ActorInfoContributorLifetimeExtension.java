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

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActorInfoContributorLifetimeExtension implements LifetimeExtension, InfoContributor
{
    private final Function<AbstractActor, Class> actorTypeResolver;

    private Set<ActorInfoContributorReference> actorInfoContributorReferences = new HashSet<>();

    ActorInfoContributorLifetimeExtension(final Function<AbstractActor, Class> actorTypeResolver)
    {
        this.actorTypeResolver = actorTypeResolver;
    }

    @Override
    public Task<?> preActivation(final AbstractActor<?> actor)
    {
        return doIfInfoContributor(actor, actorInfoContributorReferences::add);
    }

    @Override
    public Task<?> preDeactivation(final AbstractActor<?> actor)
    {
        return doIfInfoContributor(actor, actorInfoContributorReferences::remove);
    }

    private Task<?> doIfInfoContributor(final AbstractActor<?> actor,
                                        final Consumer<ActorInfoContributorReference> synchronizedAction)
    {
        if (actor instanceof InfoContributor)
        {
            ActorInfoContributorReference actorInfoContributorReference = new ActorInfoContributorReference(actor);
            synchronized(this)
            {
                synchronizedAction.accept(actorInfoContributorReference);
            }
        }
        return Task.done();
    }

    @Override
    public void contribute(final Info.Builder builder)
    {
        createInfoObjectAndRemoveExpiredReferences()
                .ifPresent(actorInfoObject -> builder.withDetail("actors", actorInfoObject));
    }

    private synchronized Optional<?> createInfoObjectAndRemoveExpiredReferences()
    {
        Map<String, Map<String, Object>> actorInfoObject = new HashMap<>();
        for (Iterator<ActorInfoContributorReference> it = actorInfoContributorReferences.iterator(); it.hasNext(); )
        {
            ActorInfoContributorReference actor = it.next();
            InfoContributor contributor = actor.reference.get();
            if (contributor == null)
            {
                it.remove();
            }
            else
            {
                final Info.Builder builder = new Info.Builder();
                contributor.contribute(builder);
                actorInfoObject.putIfAbsent(actor.name, new HashMap<>());
                actorInfoObject.get(actor.name).put(actor.identity, builder.build().getDetails());
            }
        }
        return actorInfoObject.isEmpty() ? Optional.empty() : Optional.of(actorInfoObject);
    }

    private class ActorInfoContributorReference
    {
        private final WeakReference<InfoContributor> reference;
        private final String name;
        private final String identity;

        private ActorInfoContributorReference(final AbstractActor<?> actor)
        {
            reference = new WeakReference<>((InfoContributor) actor);
            name = actorTypeResolver.apply(actor).getSimpleName();
            identity = ((Actor) actor).getIdentity();
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            final ActorInfoContributorReference that = (ActorInfoContributorReference) o;
            return name.equals(that.name) && identity.equals(that.identity);
        }

        @Override
        public int hashCode()
        {
            return name.hashCode() ^ identity.hashCode();
        }
    }
}
