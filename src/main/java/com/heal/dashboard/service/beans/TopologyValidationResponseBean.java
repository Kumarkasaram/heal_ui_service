package com.heal.dashboard.service.beans;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TopologyValidationResponseBean {

	private List<Nodes> nodeList = null;
	private List<Edges> edgesList =null;
	private String serviceId;
	
}
