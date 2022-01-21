package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class MasterKPIDetailsBean {
	 private int id;
	    private String name;
	    private String description;
	    private String dataType;
	    private int isCustom;
	    private int status;
	    private int kpiTypeId;
	    private String measureUnits;
	    private String clusterOperation;
	    private String createdTime;
	    private String updatedTime;
	    private String userDetailsId;
	    private int accountId;
	    private int kpiGroupId;
	    private String identifier;
	    private String valueType;
}
