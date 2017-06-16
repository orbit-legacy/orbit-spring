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
import org.springframework.boot.actuate.health.Status;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.cluster.NodeAddressImpl;
import cloud.orbit.actors.runtime.NodeCapabilities;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrbitHealthIndicatorTest
{
    private Stage stage;
    private OrbitHealthIndicator orbitHealthIndicator;
    private NodeAddressImpl localAddress;

    @Before
    public void setUp() throws Exception
    {
        stage = mock(Stage.class);
        orbitHealthIndicator = new OrbitHealthIndicator(stage);
        localAddress = new NodeAddressImpl(UUID.randomUUID());
        when(stage.getLocalAddress()).thenReturn(localAddress);
    }

    @Test
    public void nullStage_unhealthy() throws Exception
    {
        orbitHealthIndicator = new OrbitHealthIndicator(null);
        assertThat(orbitHealthIndicator.health().getStatus(), equalTo(Status.DOWN));
        assertThat(orbitHealthIndicator.health().getDetails(), equalTo(Collections.EMPTY_MAP));
    }

    @Test
    public void noNodes_unhealthy() throws Exception
    {
        when(stage.getAllNodes()).thenReturn(Collections.emptyList());
        when(stage.getState()).thenReturn(NodeCapabilities.NodeState.RUNNING);
        assertThat(orbitHealthIndicator.health().getStatus(), equalTo(Status.DOWN));
        assertThat(orbitHealthIndicator.health().getDetails().get("state"),
                equalTo(NodeCapabilities.NodeState.RUNNING));
        assertThat(orbitHealthIndicator.health().getDetails().get("alive"), equalTo(false));
    }

    @Test
    public void stageIsStopped_unhealthy() throws Exception
    {
        when(stage.getAllNodes()).thenReturn(Collections.singletonList(localAddress));
        when(stage.getState()).thenReturn(NodeCapabilities.NodeState.STOPPED);
        assertThat(orbitHealthIndicator.health().getStatus(), equalTo(Status.DOWN));
        assertThat(orbitHealthIndicator.health().getDetails().get("state"),
                equalTo(NodeCapabilities.NodeState.STOPPED));
        assertThat(orbitHealthIndicator.health().getDetails().get("alive"), equalTo(true));
    }

    @Test
    public void stageRunningAndNodeActive_healthy() throws Exception
    {
        when(stage.getAllNodes()).thenReturn(Collections.singletonList(localAddress));
        when(stage.getState()).thenReturn(NodeCapabilities.NodeState.RUNNING);
        assertThat(orbitHealthIndicator.health().getStatus(), equalTo(Status.UP));
        assertThat(orbitHealthIndicator.health().getDetails().get("state"),
                equalTo(NodeCapabilities.NodeState.RUNNING));
        assertThat(orbitHealthIndicator.health().getDetails().get("alive"), equalTo(true));
    }
}
