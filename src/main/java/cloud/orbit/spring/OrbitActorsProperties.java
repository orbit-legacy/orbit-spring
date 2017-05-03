/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.
 */

package cloud.orbit.spring;

import cloud.orbit.actors.Stage;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "orbit.actors")
public class OrbitActorsProperties {
    private String clusterName;
    private String nodeName;
    private Stage.StageMode stageMode;
    private Long messagingTimeoutInMillis;
    private Long timeToLiveInSeconds;
    private List<String> stickyHeaders;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String name) {
        nodeName = nodeName;
    }

    public Stage.StageMode getStageMode() {
        return stageMode;
    }

    public void setStageMode(Stage.StageMode mode) {
        this.stageMode = mode;
    }

    public Long getMessagingTimeoutInMillis() {
        return messagingTimeoutInMillis;
    }

    public void setMessagingTimeoutInMillis(Long timeout) {
        this.messagingTimeoutInMillis = timeout;
    }

    public Long getTimeToLiveInSeconds() {
        return timeToLiveInSeconds;
    }

    public void setTimeToLiveInSeconds(Long ttl) {
        this.timeToLiveInSeconds = ttl;
    }

    public List<String> getStickyHeaders() {
        return stickyHeaders;
    }

    public void setStickyHeaders(List<String> headers) {
        this.stickyHeaders = headers;
    }
}
