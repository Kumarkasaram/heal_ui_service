package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class ComponentKpis {
    private int componentId;
    private int mstCommonVersionId;
    private int kpiId;
    private String kpiName;
    private String kpiIdentifier;
    private boolean doAnalytics;
    private int status;
}
