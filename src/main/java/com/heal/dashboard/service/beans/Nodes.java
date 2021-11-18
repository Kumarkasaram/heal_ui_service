package com.heal.dashboard.service.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
public class Nodes {
    private String id;
    private String name;
    private String identifier;
    private String type;
    private String title;
    private boolean isMaintenance;
    private List<String> applicationName;
    private boolean userAccessible;
    private int workloadEventCount = -1;
    private int behaviorEventCount = -1;
    private boolean isRCAPathNode = false;
    private boolean isStartNode = false;
    private boolean entryPointNode = false;
    private boolean isServiceAffected = false;
    private Map<String, Object> metadata = new HashMap<>();
    private List<TimezoneDetail> timezoneData = new ArrayList<>();

    public Nodes() {
    }

    public Nodes(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Nodes(String serviceId) {
        this.id = serviceId;
    }

    public void addToMetaData(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nodes)) return false;
        Nodes nodes = (Nodes) o;
        return Objects.equals(this.id, nodes.id) &&
                Objects.equals(this.name, nodes.name) &&
                Objects.equals(this.type, nodes.type);
    }

    @Override
    public int hashCode() {

        return Objects.hash(this.id, this.name, this.type);
    }
}


