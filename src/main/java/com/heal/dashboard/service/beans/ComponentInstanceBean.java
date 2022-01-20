package com.heal.dashboard.service.beans;

import lombok.Data;

import java.util.List;

@Data
public class ComponentInstanceBean {
    private int id;
    private String name;
    private int status;
    private int hostId;
    private int isDR;
    private int isCluster;
    private int mstComponentVersionId;
    private String createdTime;
    private String updatedTime;
    private String userDetailsId;
    private int accountId;
    private int mstComponentId;
    private int mstComponentTypeId;
    private String identifier;
    private int discovery;
    private String hostAddress;
    private int mstCommonVersionId;
   // private List<CompInstanceKpiDetailsBean> nonGroupKpi;
   // private List<CompInstanceKpiGroupDetailsBean> groupKpi;
    private List<CompInstanceAttributesBean> attributes;
}
