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

import org.springframework.boot.actuate.autoconfigure.ConditionalOnEnabledInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cloud.orbit.actors.runtime.RemoteReference;

@Configuration
@ConditionalOnClass(InfoContributor.class)
@ConditionalOnEnabledInfoContributor("actors")
public class ActorInfoContributorConfiguration
{
    @Bean
    @ConditionalOnMissingBean(ActorInfoDetailsContainer.GroupProperties.class)
    @ConfigurationProperties(prefix = "management.info.actors.group")
    public ActorInfoDetailsContainer.GroupProperties actorInfoContainerProperties()
    {
        return new ActorInfoDetailsContainer.GroupProperties();
    }

    @Bean
    @ConditionalOnMissingBean(ActorInfoContributorLifetimeExtension.class)
    public ActorInfoContributorLifetimeExtension actorInfoContributorLifetimeExtension(
            final ActorInfoDetailsContainer.GroupProperties groupProperties)
    {
        return new ActorInfoContributorLifetimeExtension(
                RemoteReference::getInterfaceClass,
                new ActorInfoDetailsContainer(groupProperties));
    }
}
