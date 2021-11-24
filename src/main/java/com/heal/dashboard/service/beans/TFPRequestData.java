package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class TFPRequestData {
		private AccountBean account;
	    private Controller application;
	    private UserAccessDetails userAccessDetails;
	    private long fromTime;
	    private long toTime;
}
