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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActorInfoContributorLifetimeExtension implements LifetimeExtension, InfoContributor
{
    private static final Logger log = LoggerFactory.getLogger(ActorInfoContributorLifetimeExtension.class);

    private final Function<AbstractActor, Class> actorTypeResolver;
    private final ActorInfoDetailsContainer actorInfoDetailsContainer;

    private Set<ActorInfoContributorReference> actorInfoContributorReferences = new HashSet<>();

    ActorInfoContributorLifetimeExtension(final Function<AbstractActor, Class> actorTypeResolver,
                                          final ActorInfoDetailsContainer actorInfoDetailsContainer)
    {
        this.actorTypeResolver = actorTypeResolver;
        this.actorInfoDetailsContainer = actorInfoDetailsContainer;
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
            ActorInfoContributorReference actorInfoContributorReference =
                    new ActorInfoContributorReference(actorTypeResolver, actor);
            synchronized(this)
            {
                synchronizedAction.accept(actorInfoContributorReference);
            }
        }
        return Task.done();
    }

    @Override
    public synchronized void contribute(final Info.Builder builder)
    {
        Map<String, Object> details = actorInfoDetailsContainer.getDetails();
        details.clear();
        populateActorInfoContainerWhileRemovingExpiredReferences();
        if (!details.isEmpty())
        {
            builder.withDetail("actors", details);
        }
    }

    private void populateActorInfoContainerWhileRemovingExpiredReferences()
    {
        for (Iterator<ActorInfoContributorReference> it = actorInfoContributorReferences.iterator(); it.hasNext(); )
        {
            try
            {
                ActorInfoContributorReference actor = it.next();
                actorInfoDetailsContainer.mergeDetailsFrom(actor);
            }
            catch (ActorInfoContributorReference.ExpiredReferenceException e)
            {
                log.debug("Lost reference to actor", e);
                it.remove();
            }
        }
    }
}
