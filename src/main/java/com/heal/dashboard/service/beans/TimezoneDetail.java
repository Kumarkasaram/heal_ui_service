package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class TimezoneDetail {
		private int id;
	    private String timeZoneName;
	    private Long timeOffset;
	    private String createdTime;
	    private String updatedTime;
	    private String userDetailsId;
	    private Integer accountId;
	    private String offsetName;
	    private String abbreviation;
	    private Integer status;
	    private Integer objectId;
}
