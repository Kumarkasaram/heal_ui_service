package com.heal.dashboard.service.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.Edges;
import com.heal.dashboard.service.beans.Nodes;
import com.heal.dashboard.service.beans.TopologyDetails;
import com.heal.dashboard.service.beans.TopologyValidationResponseBean;
import com.heal.dashboard.service.businesslogic.ServiceDetailsBL;
import com.heal.dashboard.service.util.JsonFileParser;

@RunWith(SpringRunner.class)
public class ServiceControllerTest {

	@InjectMocks
	ServicesController ServicesController;

	@Mock
	JsonFileParser headersParser;
	@Mock
	ServiceDetailsBL serviceDetailBL;

	private TopologyDetails topologyDetails;
	private TopologyValidationResponseBean topologyResponse;
	private List<Nodes> nodeslist;
	private List<Edges> edgeslist;

	@Before
	public void setup() {

		// setting up mock data in NodesList
		nodeslist = new ArrayList<Nodes>();
		edgeslist = new ArrayList<>();

		Nodes nodes = new Nodes();
		nodes.setId("1");
		nodes.setIdentifier("qa-d681ef13-d690-4917-jkhg-6c79b-1");
		nodes.setName("test");
		nodes.setStartNode(true);
		nodeslist.add(nodes);

		// setting up mock data in EdgesList
		Edges edges = new Edges();
		edges.setSource("test");
		edges.setTarget("1");
		edges.setData(new HashMap<String, String>());
		edgeslist.add(edges);

		// setting up mock data in topologyResponse
		topologyResponse = new TopologyValidationResponseBean(nodeslist, edgeslist, "1");
		topologyDetails = new TopologyDetails();
		topologyDetails.setEdges(edgeslist);
		topologyDetails.setNodes(nodeslist);
	}

	@Test
	public void getServiceDetails() throws Exception {

		Mockito.when(serviceDetailBL.clientValidation(Mockito.any()))
				.thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
		Mockito.when(serviceDetailBL.serverValidation(Mockito.any())).thenReturn(topologyResponse);
		Mockito.when(serviceDetailBL.process(Mockito.any())).thenReturn(topologyDetails);
		Assert.assertEquals(HttpStatus.OK, ServicesController.getServiceDetails("testing", "", "", "").getStatusCode());
	}
}
