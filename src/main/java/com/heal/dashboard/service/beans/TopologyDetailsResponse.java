package com.heal.dashboard.service.beans;


import lombok.Data;

@Data
public class TopologyDetailsResponse {
	 	String responseStatus;
	    String responseMessage;
	    TopologyDetails data;
}
