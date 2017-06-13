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
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClientException;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.NodeCapabilities;
import cloud.orbit.spring.OrbitBeanDefinitionRegistrar;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = OrbitBeanDefinitionRegistrar.class)
@EnableAutoConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OrbitHealthIndicatorIntegrationTest
{
    @LocalManagementPort
    private int port;

    @Autowired
    private Stage stage;

    @Autowired
    private TestRestTemplate restTemplate;

    @After
    public void tearDown() throws Exception
    {
        // The @DirtiesContext annotation doesn't take care of the stage, so we do it explicitly
        if (stage.getState() == NodeCapabilities.NodeState.RUNNING)
        {
            stage.stop().join();
        }
    }

    @Test
    public void stageIsActive_healthCheckOk() throws Exception
    {
        HealthResponse health = getHealth();
        assertThat(health.status, equalTo(Status.UP));
        assertThat(health.orbit.alive, equalTo(true));
        assertThat(health.orbit.state, equalTo(NodeCapabilities.NodeState.RUNNING));
    }

    @Test
    public void stageIsNotActive_healthCheckNotOk() throws Exception
    {
        stage.stop().join();
        HealthResponse health = getHealth();
        assertThat(health.status, equalTo(Status.DOWN));
        assertThat(health.orbit.alive, equalTo(true)); // Orbit Hosting does not get updated after calling stage.stop()
        assertThat(health.orbit.state, equalTo(NodeCapabilities.NodeState.STOPPED));
    }

    private HealthResponse getHealth() throws RestClientException
    {
        return restTemplate.getForObject(String.format("http://localhost:%d/health", port), HealthResponse.class);
    }

    public static class HealthResponse
    {
        public Status status;
        public OrbitDetails orbit;

        public static class OrbitDetails
        {
            public boolean alive;
            public NodeCapabilities.NodeState state;
        }
    }
}
