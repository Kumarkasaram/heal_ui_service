package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class SignalRequestPojo {
	private String signalId;
	private String status;
	private Long fromTime;
	private Long toTime;
	private Long actualFromTime;

}
