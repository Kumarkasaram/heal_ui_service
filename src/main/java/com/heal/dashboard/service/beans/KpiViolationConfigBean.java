package com.heal.dashboard.service.beans;


import lombok.Data;

import java.sql.Timestamp;

@Data
public class KpiViolationConfigBean {
		private String profileName;
	    private String profileId;
	    private String operation;
	    private float minThreshold;
	    private float maxThreshold;
	    private Timestamp startTime;
	    private Timestamp endTime;
}
