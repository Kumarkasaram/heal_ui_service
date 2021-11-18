package com.heal.dashboard.service.beans;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TopologyDetails {
    List<String> impactedServiceName = new ArrayList<>();
    List<Nodes> nodes;
    List<Edges> edges;
}

