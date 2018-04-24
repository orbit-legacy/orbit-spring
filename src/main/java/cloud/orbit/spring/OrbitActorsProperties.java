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

import org.springframework.boot.context.properties.ConfigurationProperties;

import cloud.orbit.actors.Stage;

import java.util.List;

@ConfigurationProperties(prefix = "orbit.actors")
public class OrbitActorsProperties
{
    private List<String> basePackages;
    private String clusterName;
    private String placementGroup;
    private String nodeName;
    private Stage.StageMode stageMode;
    private Long messagingTimeoutInMilliseconds;
    private Long timeToLiveInSeconds;
    private List<String> stickyHeaders;
    private Integer concurrentDeactivations;
    private Long deactivationTimeoutInMilliseconds;
    private Integer executionPoolSize;
    private Long localAddressCacheTTLInMilliseconds;
    private Integer localAddressCacheMaximumSize;
    private Boolean broadcastActorDeactivations;
    private Boolean enableShutdownHook;
    private Boolean autostartStage = Boolean.TRUE;


    public List<String> getBasePackages()
    {
        return basePackages;
    }

    public void setBasePackages(final List<String> basePackages)
    {
        this.basePackages = basePackages;
    }

    public String getClusterName()
    {
        return clusterName;
    }

    public void setClusterName(String clusterName)
    {
        this.clusterName = clusterName;
    }

    public String getPlacementGroup() {
        return placementGroup;
    }

    public void setPlacementGroup(String placementGroup) {
        this.placementGroup = placementGroup;
    }

    public String getNodeName()
    {
        return nodeName;
    }

    public void setNodeName(String nodeName)
    {
        this.nodeName = nodeName;
    }

    public Stage.StageMode getStageMode()
    {
        return stageMode;
    }

    public void setStageMode(Stage.StageMode stageMode)
    {
        this.stageMode = stageMode;
    }

    public Long getMessagingTimeoutInMilliseconds()
    {
        return messagingTimeoutInMilliseconds;
    }

    public void setMessagingTimeoutInMilliseconds(Long timeout)
    {
        this.messagingTimeoutInMilliseconds = timeout;
    }

    public Long getTimeToLiveInSeconds()
    {
        return timeToLiveInSeconds;
    }

    public void setTimeToLiveInSeconds(Long ttl)
    {
        this.timeToLiveInSeconds = ttl;
    }

    public List<String> getStickyHeaders()
    {
        return stickyHeaders;
    }

    public void setStickyHeaders(List<String> headers)
    {
        this.stickyHeaders = headers;
    }

    public Integer getConcurrentDeactivations()
    {
        return concurrentDeactivations;
    }

    public void setConcurrentDeactivations(final Integer concurrentDeactivations)
    {
        this.concurrentDeactivations = concurrentDeactivations;
    }

    public Long getDeactivationTimeoutInMilliseconds()
    {
        return deactivationTimeoutInMilliseconds;
    }

    public void setDeactivationTimeoutInMilliseconds(final Long deactivationTimeoutInMilliseconds)
    {
        this.deactivationTimeoutInMilliseconds = deactivationTimeoutInMilliseconds;
    }

    public Integer getExecutionPoolSize()
    {
        return executionPoolSize;
    }

    public void setExecutionPoolSize(Integer executionPoolSize)
    {
        this.executionPoolSize = executionPoolSize;
    }

    public Long getLocalAddressCacheTTLInMilliseconds()
    {
        return localAddressCacheTTLInMilliseconds;
    }

    public void setLocalAddressCacheTTLInMilliseconds(Long localAddressCacheTTLInMilliseconds)
    {
        this.localAddressCacheTTLInMilliseconds = localAddressCacheTTLInMilliseconds;
    }

    public Integer getLocalAddressCacheMaximumSize()
    {
        return localAddressCacheMaximumSize;
    }

    public void setLocalAddressCacheMaximumSize(Integer localAddressCacheMaximumSize)
    {
        this.localAddressCacheMaximumSize = localAddressCacheMaximumSize;
    }

    public Boolean getBroadcastActorDeactivations()
    {
        return broadcastActorDeactivations;
    }

    public void setBroadcastActorDeactivations(Boolean broadcastActorDeactivations)
    {
        this.broadcastActorDeactivations = broadcastActorDeactivations;
    }

    public Boolean getEnableShutdownHook()
    {
        return enableShutdownHook;
    }

    public void setEnableShutdownHook(Boolean enableShutdownHook)
    {
        this.enableShutdownHook = enableShutdownHook;
    }

    public Boolean getAutostartStage()
    {
        return autostartStage;
    }

    public void setAutostartStage(Boolean autostartStage)
    {
        this.autostartStage = autostartStage;
    }


}
