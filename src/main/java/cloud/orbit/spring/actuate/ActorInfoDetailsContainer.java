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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

class ActorInfoDetailsContainer
{
    private final List<Function<ActorInfoContributorReference, String>> detailLevelHandlers = new ArrayList<>();
    private final Map<String, Object> details = new HashMap<>();

    ActorInfoDetailsContainer(GroupProperties groupProperties)
    {
        Stream.of(groupProperties.primary, groupProperties.secondary)
                .map(this::deriveHandlerFromGroupType)
                .forEach(handler -> handler.ifPresent(detailLevelHandlers::add));
    }

    private Optional<Function<ActorInfoContributorReference, String>> deriveHandlerFromGroupType(
            final GroupProperties.GroupType groupType) {
        if (groupType == GroupProperties.GroupType.IDENTITY)
        {
            return Optional.of(ActorInfoContributorReference::getIdentity);
        }
        if (groupType == GroupProperties.GroupType.INTERFACE)
        {
            return Optional.of(ActorInfoContributorReference::getName);
        }
        return Optional.empty();
    }

    void mergeDetailsFrom(ActorInfoContributorReference actorInfoContributorReference)
    {
        Info.Builder builder = new Info.Builder();
        actorInfoContributorReference.getInfoContributor().contribute(builder);
        Map<String, Object> whereToPutDetails = details;
        for (Function<ActorInfoContributorReference, String> handler: detailLevelHandlers)
        {
            String key = handler.apply(actorInfoContributorReference);
            whereToPutDetails.putIfAbsent(key, new HashMap<String, Object>());
            //noinspection unchecked
            whereToPutDetails = (Map<String, Object>) whereToPutDetails.get(key);
        }
        whereToPutDetails.putAll(builder.build().getDetails());
    }

    Map<String, Object> getDetails()
    {
        return details;
    }

    @SuppressWarnings({ "WeakerAccess", "unused" }) // public setters needed for Spring to inject properties
    static class GroupProperties
    {
        private GroupType primary = GroupType.INTERFACE;
        private GroupType secondary = GroupType.IDENTITY;

        public void setPrimary(final GroupType primary)
        {
            this.primary = primary;
        }

        public void setSecondary(final GroupType secondary)
        {
            this.secondary = secondary;
        }

        @Override
        public String toString()
        {
            return "GroupProperties{" +
                    "primary=" + primary +
                    ", secondary=" + secondary +
                    '}';
        }

        enum GroupType
        {
            NONE, INTERFACE, IDENTITY
        }
    }
}
