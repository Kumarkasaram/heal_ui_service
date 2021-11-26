package com.heal.dashboard.service.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationSDMBean {

		UserAccessDetails userAccessDetails;
	    AccountBean accountBean;
	    Controller controller;
	    long fromTime;
	    long toTime;
}
