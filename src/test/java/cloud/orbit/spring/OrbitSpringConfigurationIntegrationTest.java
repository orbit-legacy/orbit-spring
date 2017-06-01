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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.extensions.ActorExtension;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/*
 * Very simple (poor) tests to validate that the SpringActorConstructionExtension is being properly bound to the Stage
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ContextConfiguration(classes = OrbitSpringConfiguration.class)
public class OrbitSpringConfigurationIntegrationTest
{
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void loadSpringActorConstructionExtension() throws Exception
    {
        Map<String, ActorExtension> actorExtensionBeans = applicationContext.getBeansOfType(ActorExtension.class);
        assertTrue(actorExtensionBeans.keySet().contains("springActorConstructionExtension"));
    }

    @Test
    public void validateStageHasSpringActorConstructionExtension() throws Exception
    {
        Stage stage = applicationContext.getBean(Stage.class);
        List<ActorExtension> actorExtensions = stage.getAllExtensions(ActorExtension.class);
        assertTrue(actorExtensions.stream().anyMatch((p -> p instanceof SpringActorConstructionExtension)));
    }
}
