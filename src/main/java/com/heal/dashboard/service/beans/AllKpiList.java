package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class AllKpiList {
    private int kpiId;
    private String kpiName;
    private String kpiDescription;
    private String kpiUnits;
    private String clusterOperation;
    private String kpiType;
    private int groupId;
    private String groupName;
    private String groupIdentifier;
    private int isDiscovery;
    private int groupStatus;
    private String rollupOperation;
    private int clusterAggType;
    private int instanceAggType;
    private String identifier;
}
