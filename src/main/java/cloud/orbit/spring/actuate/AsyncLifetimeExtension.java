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

import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

/**
 * Provides hooks for an actor's activation and deactivation that are visited asynchronously, separate from the main
 * lifecycle of the actor. Use this abstract class for expensive actor processing so that it does not block the
 * activation and deactivation of an actor. Warning: Due to its asynchronous nature, there is no guarantee that the
 * async hooks are called.
 */
public abstract class AsyncLifetimeExtension implements LifetimeExtension
{
    private static final Logger log = LoggerFactory.getLogger(AsyncLifetimeExtension.class);
    private final Executor executor;

    public AsyncLifetimeExtension(final Executor executor)
    {
        this.executor = executor;
    }

    @Override
    public Task<?> postActivation(final AbstractActor<?> actor)
    {
        executor.execute(new AsyncJob(actor, true));
        return Task.done();
    }

    @Override
    public Task<?> postDeactivation(final AbstractActor<?> actor)
    {
        executor.execute(new AsyncJob(actor, false));
        return Task.done();
    }

    protected abstract Task postActivationAsync(final AbstractActor<?> actor);

    protected abstract Task postDeactivationAsync(final AbstractActor<?> actor);

    private class AsyncJob implements Runnable {
        private final WeakReference<AbstractActor<?>> actorReference;
        private final boolean isActivation;

        private AsyncJob(final AbstractActor<?> actor, final boolean isActivation)
        {
            this.actorReference = new WeakReference<>(actor);
            this.isActivation = isActivation;
        }

        @Override
        public void run()
        {
            AbstractActor<?> actor = actorReference.get();
            if (actor == null)
            {
                log.debug("Lost reference to actor. Skipping background lifecycle handling.");
            }
            else
            {
                if (isActivation)
                {
                    postActivationAsync(actor).join();
                }
                else
                {
                    postDeactivationAsync(actor).join();
                }
            }
        }
    }
}
