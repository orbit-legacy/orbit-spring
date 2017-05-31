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

import org.junit.Before;
import org.junit.Test;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.extensions.ActorExtension;

import java.util.Collections;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class OrbitSpringConfigurationTest
{
    private OrbitActorsProperties properties;

    @Before
    public void setUp() throws Exception
    {
        properties = new OrbitActorsProperties();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildStage_nullProperties_throws() throws Exception
    {
        new OrbitSpringConfiguration().buildStage(null, null, null, null);
    }

    @Test
    public void buildStage_nullExtensionsAndNullMessagingAndNoProperties_ok() throws Exception
    {
        new OrbitSpringConfiguration().buildStage(properties, null, null, null);
    }

    @Test
    public void buildStage_withAnExtension_stageHasExtension() throws Exception
    {
        ActorExtension actorExtension = mock(ActorExtension.class);
        Stage stage = new OrbitSpringConfiguration()
                .buildStage(properties, Collections.singletonList(actorExtension), null, null);
        assertThat(stage.getExtensions(), contains(actorExtension));
    }
}