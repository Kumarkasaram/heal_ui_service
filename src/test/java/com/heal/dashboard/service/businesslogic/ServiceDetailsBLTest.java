package com.heal.dashboard.service.businesslogic;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.TxnAndGroupBean;
import com.heal.dashboard.service.beans.UserAccessBean;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.Edges;
import com.heal.dashboard.service.beans.Nodes;
import com.heal.dashboard.service.beans.TopologyValidationResponseBean;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.TopologyUtility;
import com.heal.dashboard.service.util.UserValidationUtil;

@RunWith(SpringRunner.class)
public class ServiceDetailsBLTest {
		
		@InjectMocks
	    ServiceDetailsBL  serviceDetailsBL;
	    @Mock
	    AccountDao acountDao;
	    @Mock
	    TopologyUtility topologyUtiliBL; 
	    @Mock
	    CommonServiceBL commonServiceBL;
	    @Mock
	    List<AccountBean> accountBeansList;
	    @Mock
	    UserAccessDetails userAccessDetails;
	    @Mock
		ControllerDao controllerDao;
		@Mock
		private UserValidationUtil userValidationUtil;

	    private List<TxnAndGroupBean> txnAndGroupList;
	    private UserAccessBean userAccessBean;
	    private List<Controller> controllerBeanList ;
	    private  List<Nodes> nodeslist;
	    private  List<Edges> edgeslist ;
	    private TopologyValidationResponseBean topologyResponse;
	    private AccountBean  accountBean;
	    
	    @Before
	    public void setup() {
//	      setting up mock data in  accountBean
	        accountBeansList = new ArrayList<>();
	        accountBean = new AccountBean();
	        accountBean.setId(2);;
	        accountBean.setIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
	        accountBean.setName("India");
	        accountBeansList.add(accountBean);

//	      setting up mock data in  userAccessDetails
	        userAccessDetails = new UserAccessDetails();
	        String [] identifier  = {"7640123a-fbde-4fe5-9812-581cd1e3a9c1","a-d681ef13-d690-4917-jkhg-6c79b-1"};
	        List<String> identifierList = new ArrayList<>();
	        identifierList.add("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
	        identifierList.add("a-d681ef13-d690-4917-jkhg-6c79b-2");
	        userAccessDetails.setApplicationIdentifiers(identifierList);
	        
//	      setting up mock data in  userAccessDetails
	        userAccessBean = new UserAccessBean();
	        userAccessBean.setAccessDetails("{\"accounts\": [\"*\"]}");
	        userAccessBean.setId(1);
	        userAccessBean.setUpdatedTime(LocalDateTime.now());
	        userAccessBean.setCreatedTime(LocalDateTime.now());
	        userAccessBean.setUserIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
	        
//	      setting up mock data in  TxnAndGroupBean
	        txnAndGroupList  = new ArrayList<>();
	        TxnAndGroupBean txnAndGroupBean = new TxnAndGroupBean();
	        txnAndGroupBean.setIdentifier("qa-d681ef13-d690-4917-jkhg-6c79b-1");
	        txnAndGroupBean.setDescription("test");
	        txnAndGroupBean.setIsAutoConfigured(1);
	        txnAndGroupBean.setServiceId("1");
	        txnAndGroupList.add(txnAndGroupBean);
	        
//	      setting up mock data in  Controller
	        Controller controller = new Controller();
	        controller.setAccountId(1);
	        controller.setAppId("2");
	        controller.setName("test");
	        controller.setStatus(1);
	        controller.setControllerTypeId(1);
	        controller.setIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
	        controllerBeanList = new ArrayList<Controller>();
	        controllerBeanList.add(controller);
	        
	      
	         nodeslist = new ArrayList<Nodes>();
	         edgeslist = new ArrayList<>();
	         
	         Nodes nodes = new Nodes();
	         nodes.setId("1");
	         nodes.setIdentifier("qa-d681ef13-d690-4917-jkhg-6c79b-1");
	         nodes.setName("test");
	         nodes.setStartNode(true);
	         nodeslist.add(nodes);
	         
	         Edges edges = new Edges();
	         edges.setSource("test");
	         edges.setTarget("1");
	         edges.setData(new HashMap<String, String>());
	         edgeslist.add(edges);
	         
	         topologyResponse = new TopologyValidationResponseBean(nodeslist, edgeslist, "1");
	    }	

	    
	    @Test
	    public void getClientValidation_Success() throws Exception {
	        Map<String,String> params = new HashMap<String,String>(); 
	        Assert.assertEquals("7640123a-fbde-4fe5-9812-581cd1e3a9c1", serviceDetailsBL.clientValidation(null,"7640123a-fbde-4fe5-9812-581cd1e3a9c1","2","1","7640123a-fbde-4fe5-9812-581cd1e3a9c1").getAuthToken());
	    }
	    
	    @Test(expected = ClientException.class)  
	    public void getClientValidation_ClientException1() throws ClientException {
	        serviceDetailsBL.clientValidation(null,"");
	      
	    }

	    @Test
	    public void serverValidation() throws Exception {
	    	  Map<String,String> params = new HashMap<String,String>();
		        params.put("identifier", "7640123a-fbde-4fe5-9812-581cd1e3a9c1");
		        params.put("serviceId", "2");
		        params.put("ndegree", "1");
		    	UtilityBean<String> utilityBean = UtilityBean.<String>builder().authToken("7640123a-fbde-4fe5-9812-581cd1e3a9c1").accountIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build();

		    	Mockito.when(acountDao.fetchUserAccessDetailsUsingIdentifier(Mockito.anyString())).thenReturn(userAccessBean);
				Mockito.when(acountDao.getAccountDetailsForIdentifier(Mockito.anyString()))
						.thenReturn(accountBean);
				Mockito.when(userValidationUtil.getUserAccessDetails(Mockito.anyString(), Mockito.anyString())).thenReturn(new UserAccessDetails());
				Mockito.when(controllerDao.getAllServicesForAccount(Mockito.anyInt()))
				.thenReturn(new ArrayList<>());
		        Mockito.when(commonServiceBL.extractUserAccessDetails(Mockito.anyString(), Mockito.any())).thenReturn(userAccessDetails);
		    	Mockito.when(topologyUtiliBL.getNodeList(Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.anyLong()))
				.thenReturn(nodeslist);
		Mockito.when(topologyUtiliBL.getEdgeList(Mockito.anyInt(),Mockito.anyList())).thenReturn(edgeslist);
		     Assert.assertEquals(nodeslist, serviceDetailsBL.serverValidation(utilityBean).getNodeList());
	    }

	    @Test(expected = ServerException.class )
	    public void serverValidation_Case2() throws Exception {
	   	 UtilityBean<String> utilityBean = UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build();
	        Mockito.when(acountDao.fetchUserAccessDetailsUsingIdentifier(Mockito.anyString())).thenReturn(null);
	        Mockito.when(acountDao.getAccountDetails(Mockito.anyString(), Mockito.anyString())).thenReturn(accountBeansList);
	        Mockito.when(commonServiceBL.extractUserAccessDetails(Mockito.anyString(), Mockito.any())).thenReturn(userAccessDetails);
	    	Mockito.when(topologyUtiliBL.getNodeList(Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.anyLong()))
			.thenReturn(nodeslist);
	    	Mockito.when(topologyUtiliBL.getEdgeList(Mockito.anyInt(),Mockito.anyList())).thenReturn(edgeslist);
	     Assert.assertEquals(1, serviceDetailsBL.serverValidation(utilityBean).getNodeList().get(0).getId());
	 }
	    
	    @Test
	    public void processData() throws Exception {
	            Assert.assertEquals(edgeslist, serviceDetailsBL.process(topologyResponse).getEdges());
	    }
	  
	
}
