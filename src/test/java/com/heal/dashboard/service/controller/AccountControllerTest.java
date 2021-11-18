package com.heal.dashboard.service.controller;

import java.time.LocalDateTime;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.AccountMappingBean;
import com.heal.dashboard.service.pojo.ApplicationDetails;
import com.heal.dashboard.service.beans.ApplicationHealthDetail;
import com.heal.dashboard.service.beans.DateComponentBean;
import com.heal.dashboard.service.beans.MasterFeatureDetails;
import com.heal.dashboard.service.beans.MasterFeaturesBean;
import com.heal.dashboard.service.beans.UserAccessAccountsBean;
import com.heal.dashboard.service.beans.UserAccessBean;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.Edges;
import com.heal.dashboard.service.beans.Nodes;
import com.heal.dashboard.service.beans.TopologyDetails;
import com.heal.dashboard.service.beans.TopologyValidationResponseBean;
import com.heal.dashboard.service.businesslogic.ApplicationHealthBL;
import com.heal.dashboard.service.businesslogic.DateComponentBL;
import com.heal.dashboard.service.businesslogic.GetAccountsBL;
import com.heal.dashboard.service.businesslogic.GetApplicationBL;
import com.heal.dashboard.service.businesslogic.MasterFeaturesBL;
import com.heal.dashboard.service.businesslogic.TopologyServiceBL;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.JsonFileParser;

@RunWith(SpringRunner.class)
public class AccountControllerTest {

    @InjectMocks
    AccountController accountcontroller;
    @InjectMocks
    ServicesController servicesController;
    @Mock
    GetAccountsBL getAccountsBL;
    @Mock
    GetApplicationBL getApplicationBL;
    @Mock
    MasterFeaturesBL masterFeaturesBL;
    @Mock
    List<AccountBean> accountBeansList;
    @Mock
    TopologyServiceBL topologyServiceBL;
    @Mock
    DateComponentBL dateComponentBL;
    @Mock
    UserAccessBean userAccessDetails;
    @Mock
    ApplicationHealthBL applicationHealthBL;
    @Mock
    JsonFileParser headersParser;
    

    TopologyDetails topologyDetails;

    List<ApplicationDetails> applicationDetailsList;

    UserAccessAccountsBean accessAccountsBean;
	TopologyValidationResponseBean topologyResponse;

    @Before
    public void setup() {
        accountBeansList = new ArrayList<>();
        AccountBean accountBean = new AccountBean();
        accountBean.setAccountId(2);
        accountBean.setIdentifier("qa-d681ef13-d690-4917-jkhg-6c79b-1");
        accountBean.setName("India");
        accountBeansList.add(accountBean);

        topologyDetails = new TopologyDetails();
        List<Nodes> nodeslist = new ArrayList<Nodes>();
        List<Edges> edgeslist = new ArrayList<>();

        Nodes nodes = new Nodes();
        nodes.setId("1");
        nodes.setIdentifier("qa-d681ef13-d690-4917-jkhg-6c79b-1");
        nodes.setName("test");
        nodes.setStartNode(true);
        nodeslist.add(nodes);

        Edges edges = new Edges();
        edges.setSource("test");
        edges.setTarget("qa-d681ef13-d690-4917-jkhg-6c79b-1");
        edges.setData(new HashMap<String, String>());
        edgeslist.add(edges);

        applicationDetailsList = new ArrayList<>();
        ApplicationDetails applicationDetails = new ApplicationDetails();
        applicationDetails.setId(1);
        applicationDetails.setIdentifier("qa-d681ef13-d690-4917-jkhg-6c79b-1");
        applicationDetails.setName("test");
        applicationDetailsList.add(applicationDetails);

        //setting up mock data in  userAccessDetails
        userAccessDetails = new UserAccessBean();
        userAccessDetails.setAccessDetails("{\"accounts\": [\"*\"]}");
        userAccessDetails.setId(1);
        userAccessDetails.setUpdatedTime(LocalDateTime.now());
        userAccessDetails.setCreatedTime(LocalDateTime.now());
        userAccessDetails.setUserIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
        accessAccountsBean = new UserAccessAccountsBean(new Gson().fromJson(userAccessDetails.getAccessDetails(), AccountMappingBean.class), accountBeansList);
    }

    @Test(expected = RuntimeException.class)
    public void getAccountListTest_InternalServerErrror() throws ServerException, ClientException, DataProcessingException {
    	Mockito.when(getAccountsBL.clientValidation(Mockito.anyObject(),Mockito.anyString()))
		.thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
		Mockito.when(getAccountsBL.serverValidation(Mockito.any())).thenReturn(accessAccountsBean);
		Mockito.when(getAccountsBL.process(Mockito.any())).thenThrow( new RuntimeException());
		Assert.assertEquals("India",
        accountcontroller.getAccountList("tetsing"));
    }

    @Test(expected = RuntimeException.class)
    public void getAccountList_InternalServerError() throws Exception {
        Mockito.when(getAccountsBL.clientValidation(Mockito.any())).thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
        Mockito.when(getAccountsBL.serverValidation(Mockito.any())).thenReturn(accessAccountsBean);
        Mockito.when(getAccountsBL.process(Mockito.any())).thenThrow(new RuntimeException());
        Assert.assertNull(accountcontroller.getAccountList("7640123a-fbde-4fe5-9812-581cd1e3a9c1"));
    }

    @Test
    public void getAccountListTest_Ok() throws Exception {
    	Mockito.when(getAccountsBL.clientValidation(Mockito.anyObject(),Mockito.anyString()))
		.thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
		Mockito.when(getAccountsBL.serverValidation(Mockito.any())).thenReturn(accessAccountsBean);
		Mockito.when(getAccountsBL.process(Mockito.any())).thenReturn(accountBeansList);
        Mockito.when(headersParser.loadHeaderConfiguration()).thenReturn(new HttpHeaders());
		Assert.assertEquals("India",
        accountcontroller.getAccountList("tetsing").getBody().get(0).getName());
    }

    @Test
    public void getAccountList_Success() throws Exception {
    	Mockito.when(getAccountsBL.clientValidation(Mockito.anyObject(),Mockito.any()))
        .thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
        Mockito.when(getAccountsBL.serverValidation(Mockito.any())).thenReturn(accessAccountsBean);
        Mockito.when(getAccountsBL.process(Mockito.any())).thenReturn(accountBeansList);
        Mockito.when(headersParser.loadHeaderConfiguration()).thenReturn(new HttpHeaders());
        Assert.assertEquals("India", accountcontroller.getAccountList("7640123a-fbde-4fe5-9812-581cd1e3a9c1").getBody().get(0).getName());
    }

    
    @Test
    public void getApplicationList_OK() throws Exception {
    	applicationDetailsList = new ArrayList<>();
		ApplicationDetails applicationDetails = new ApplicationDetails();
		applicationDetails.setId(1);
		applicationDetails.setIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
		applicationDetails.setName("test");
		applicationDetailsList.add(applicationDetails);
		Mockito.when(getApplicationBL.clientValidation(Mockito.anyObject(),Mockito.any()))
				.thenReturn(UtilityBean.<String>builder().accountIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1").authToken("7640123a-fbde-4fe5-9812-581cd1e3a9c1").pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
		Mockito.when(getApplicationBL.serverValidation(Mockito.any())).thenReturn(new UserAccessDetails());
		Mockito.when(getApplicationBL.process(Mockito.any())).thenReturn(applicationDetailsList);
        Mockito.when(headersParser.loadHeaderConfiguration()).thenReturn(new HttpHeaders());

		Assert.assertEquals("test",
				accountcontroller.getApplicationList("7640123a-fbde-4fe5-9812-581cd1e3a9c1", "").getBody().get(0).getName());
    }
    @Test
    public void getApplicationList_Success() throws Exception {
        applicationDetailsList = new ArrayList<>();
        ApplicationDetails applicationDetails = new ApplicationDetails();
        applicationDetails.setId(1);
        applicationDetails.setIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
        applicationDetails.setName("test");
        applicationDetailsList.add(applicationDetails);
        Mockito.when(getApplicationBL.clientValidation(Mockito.any())).thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
        Mockito.when(getApplicationBL.serverValidation(Mockito.any())).thenReturn(new UserAccessDetails());
        Mockito.when(headersParser.loadHeaderConfiguration()).thenReturn(new HttpHeaders());
        Mockito.when(getApplicationBL.process(Mockito.any())).thenReturn(applicationDetailsList);
        Assert.assertEquals("test", accountcontroller.getApplicationList("7640123a-fbde-4fe5-9812-581cd1e3a9c1", "").getBody().get(0).getName());
    }

    @Test
    public void getTopologyDetails() throws Exception {
    	  Mockito.when(topologyServiceBL.clientValidation(Mockito.any())).thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
	        Mockito.when(topologyServiceBL.serverValidation(Mockito.any())).thenReturn(topologyResponse);
	        Mockito.when(headersParser.loadHeaderConfiguration()).thenReturn(new HttpHeaders());
	        Mockito.when(topologyServiceBL.process(Mockito.any())).thenReturn(topologyDetails);
        Assert.assertEquals(HttpStatus.OK, accountcontroller.getTopologyDetails("testing", "", "").getStatusCode());
    }



 
     @Test
     public void getMasterFeature()throws Exception{
    	 Mockito.when(masterFeaturesBL.clientValidation(null,""))
			.thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
    	 	Mockito.when(masterFeaturesBL.serverValidation(Mockito.any())).thenReturn(new ArrayList<>());
            Mockito.when(headersParser.loadHeaderConfiguration()).thenReturn(new HttpHeaders());	
    	 	Mockito.when(masterFeaturesBL.process(Mockito.any())).thenReturn(getMasterFeatureDetails());
         Assert.assertEquals(HttpStatus.OK, accountcontroller.getMasterFeatures().getStatusCode());
     }

    

 	private MasterFeatureDetails getMasterFeatureDetails() {
		List<MasterFeaturesBean> masterFeaturesBeans = new ArrayList<>();
		MasterFeaturesBean masterFeaturesBean = new MasterFeaturesBean();
		masterFeaturesBean.setId(1);
		masterFeaturesBean.setName("Upload");
		masterFeaturesBean.setEnabled(true);
		masterFeaturesBeans.add(masterFeaturesBean);
		MasterFeatureDetails response = new MasterFeatureDetails(masterFeaturesBeans);
		return response;
	}
 	
 	/* @Test(expected = RuntimeException.class)
     public void getMasterFeatureTest_InternalServerError() {
         Mockito.when(getAccountService.getMasterFeatures()).thenThrow( new RuntimeException());
         accountcontroller.getMasterFeatures();
     }*/
     @Test
     public void getDateTimeDropdownList()throws Exception{
    	 Mockito.when(dateComponentBL.clientValidation(null,""))
			.thenReturn(UtilityBean.<String>builder().pojoObject("7640123a-fbde-4fe5-9812-581cd1e3a9c1").build());
         Mockito.when(headersParser.loadHeaderConfiguration()).thenReturn(new HttpHeaders());
    	 Mockito.when(dateComponentBL.serverValidation(Mockito.any())).thenReturn(new ArrayList<>());
	Mockito.when(dateComponentBL.process(Mockito.any())).thenReturn(getDateComponentDetail());
         Assert.assertEquals(HttpStatus.OK, accountcontroller.getDateTimeDropdownList().getStatusCode());
     }

     
     private DateComponentDetailBean getDateComponentDetail() {
 		List<DateComponentBean> dateComponentBeans = new ArrayList<>();
 		DateComponentBean dateComponentBean = new DateComponentBean();
 		dateComponentBean.setType("hours");
 		dateComponentBean.setValue(12);
 		dateComponentBean.setLabel("Last 12 hours");
 		dateComponentBeans.add(dateComponentBean);
 		DateComponentDetailBean response = new DateComponentDetailBean(dateComponentBeans);
 		return response;
 	}
//     @Test(expected = RuntimeException.class)
//     public void getDateTimeDropdownListTest_InternalServerError() {
//         Mockito.when(getAccountService.getDateTimeDropdownList()).thenThrow( new RuntimeException());
//         accountcontroller.getDateTimeDropdownList();
//     }

     @Test
 	public void getApplicationHealthStatus() throws Exception {
    	 List<ApplicationHealthDetail> applicationHealthDetailsList = new ArrayList<>();
 		ApplicationHealthDetail applicationHealthDetail = new ApplicationHealthDetail();
 		applicationHealthDetail.setId(1);
 		applicationHealthDetail.setIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
 		applicationHealthDetail.setName("test");
 		applicationHealthDetailsList.add(applicationHealthDetail);
        Mockito.when(headersParser.loadHeaderConfiguration()).thenReturn(new HttpHeaders());
 		Mockito.when(applicationHealthBL.clientValidation(Mockito.any()))
 				.thenReturn(UtilityBean.<String>builder().authToken("7640123a-fbde-4fe5-9812-581cd1e3a9c1").accountIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1").pojoObject("2").build());
 		Mockito.when(applicationHealthBL.serverValidation(Mockito.any())).thenReturn(new ApplicationHealthResponse());
 		Mockito.when(applicationHealthBL.process(Mockito.any())).thenReturn(applicationHealthDetailsList);
         Assert.assertEquals(HttpStatus.OK, accountcontroller.getApplicationHealthStatus("","","").getStatusCode());

 }
  
}
