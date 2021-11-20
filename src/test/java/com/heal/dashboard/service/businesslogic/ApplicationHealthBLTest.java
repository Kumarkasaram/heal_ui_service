package com.heal.dashboard.service.businesslogic;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;
import com.datastax.driver.core.Row;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.ApplicationHealthDetail;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.TagDetails;
import com.heal.dashboard.service.beans.TagMapping;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.AccountCassandraDao;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.dao.mysql.TagsDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.util.UserValidationUtil;
@RunWith(SpringRunner.class)
public class ApplicationHealthBLTest {

	@InjectMocks
	ApplicationHealthBL applicationHealthBL;
	@Mock
	ApplicationHealthBL mockApplicationHealthBL;
	@Mock
	AccountDao acountDao;
	@Mock
	List<AccountBean> accountBeansList;
	@Mock
	AccountCassandraDao accountSignalDao; 
	@Mock
	ControllerDao controllerDao;
	@Mock
	MasterDataDao masterDataDao;
	@Mock
	TagsDao tagsDao;

	@Mock
	UserAccessDetails userAccessDetailsmock;
	UserAccessDetails userAccessDetails;

	@Mock
	private UserValidationUtil userValidationUtil;

	private List<Controller> controllerBeanList;
	List<ApplicationHealthDetail> appHealthData;
	AccountBean accountBean ;

	@Before
	public void setup() {
		// setting up mock data in accountBean
		accountBeansList = new ArrayList<>();
		accountBean = new AccountBean();
		accountBean.setId(2);
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
	        
		// setting up mock data in Controller
		Controller controller = new Controller();
		controller.setAccountId(1);
		controller.setAppId("2");
		controller.setName("test");
		controller.setStatus(1);
		controller.setControllerTypeId(1);
		controller.setIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
		controllerBeanList = new ArrayList<Controller>();
		controllerBeanList.add(controller);
		
		ApplicationHealthDetail applicationHealthDetail = new ApplicationHealthDetail();
		applicationHealthDetail.setId(1);
		applicationHealthDetail.setIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
		applicationHealthDetail.setDashboardUId("1");
		applicationHealthDetail.setMaintenanceWindowStatus(true);
		applicationHealthDetail.setName("test");
		applicationHealthDetail.setBatch(new ArrayList<>());
		appHealthData = new ArrayList<>();
		appHealthData.add(applicationHealthDetail);
	}

	@Test
	public void getClientValidation_Success() throws Exception {
		Assert.assertEquals("7640123a-fbde-4fe5-9812-581cd1e3a9c1",
				applicationHealthBL.clientValidation(null,"7640123a-fbde-4fe5-9812-581cd1e3a9c1","2","7640123a-fbde-4fe5-9812-581cd1e3a9c1").getAuthToken());
	}

	@Test(expected = ClientException.class)
	public void getClientValidation_ClientException1() throws ClientException {
		applicationHealthBL.clientValidation(null,"");

	}

	@Test
	public void serverValidation() throws Exception {
		TagDetails tagdetail = new TagDetails();
		tagdetail.setAccountId(1);
		tagdetail.setId(1);
		tagdetail.setName("test");
		tagdetail.setTagTypeId(2);
		tagdetail.setUserDetailsId("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
		
		List<TagMapping> txnList = new ArrayList<>();
		TagMapping tagMapping = new TagMapping();
		tagMapping.setAccountId(1);
		tagMapping.setId(1);
		tagMapping.setTagId(0);
		tagMapping.setTagKey("test");
		tagMapping.setTagValue("value");
		txnList.add(tagMapping);
			
		UtilityBean<String> utilityBean = UtilityBean.<String>builder()
				.authToken("7640123a-fbde-4fe5-9812-581cd1e3a9c1")
				.accountIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1")
				.pojoObject("1").build();
		Mockito.when(acountDao.getAccountDetailsForIdentifier(Mockito.anyString()))
				.thenReturn(accountBean);
		Mockito.when(userValidationUtil.getUserAccessDetails(Mockito.anyString(), Mockito.anyString())).thenReturn(userAccessDetails);
		Mockito.when(masterDataDao.getAllViewTypes())
		.thenReturn(new ArrayList<>());
		Mockito.when(tagsDao.getTagDetails(Mockito.anyString(), Mockito.anyInt()))
		.thenReturn(tagdetail);
		Mockito.when(tagsDao.getTagMappingDetailsByAccountId(Mockito.anyInt()))
		.thenReturn(txnList);
		Assert.assertEquals(userAccessDetails.getApplicationIdentifiers(), applicationHealthBL.serverValidation(utilityBean).getApplicationIdentifiers());
	}

	@Test(expected = Exception.class)
	public void serverValidation_Case2() throws Exception {
		TagDetails tagdetail = new TagDetails();
		tagdetail.setAccountId(1);
		tagdetail.setId(1);
		tagdetail.setName("test");
		tagdetail.setTagTypeId(2);
		tagdetail.setUserDetailsId("7640123a-fbde-4fe5-9812-581cd1e3a9c1");
		
		List<TagMapping> txnList = new ArrayList<>();
		TagMapping tagMapping = new TagMapping();
		tagMapping.setAccountId(2);
		tagMapping.setId(2);
		tagMapping.setTagId(0);
		tagMapping.setTagKey("test");
		tagMapping.setTagValue("value");
		txnList.add(tagMapping);
		List<Row> signlelist = new ArrayList<>();
		UtilityBean<String> utilityBean = UtilityBean.<String>builder()
				.authToken("7640123a-fbde-4fe5-9812-581cd1e3a9c1")
				.accountIdentifier("7640123a-fbde-4fe5-9812-581cd1e3a9c1")
				.pojoObject("2").build();
		Mockito.when(acountDao.getAccountDetails(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(null);
		Mockito.when(mockApplicationHealthBL.getProblemList(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong()))
				.thenReturn(signlelist);
		Mockito.when(mockApplicationHealthBL.getOpenProblems(Mockito.any(), Mockito.anyList(), Mockito.anyList()))
				.thenReturn(appHealthData);
		Mockito.when(tagsDao.getTagDetails(Mockito.anyString(), Mockito.anyInt()))
		.thenReturn(tagdetail);
		Mockito.when(tagsDao.getTagMappingDetailsByAccountId(Mockito.anyInt()))
		.thenReturn(txnList);
		Assert.assertEquals(userAccessDetails.getApplicationIdentifiers(), applicationHealthBL.serverValidation(utilityBean).getApplicationIdentifiers());
	}

//	@Test
//	public void processData() throws Exception {
//		List<Row> signlelist = new ArrayList<>();
//		Mockito.when(mockApplicationHealthBL.getProblemList(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong()))
//		.thenReturn(signlelist);
//		Mockito.when(userAccessDetailsmock.getApplicationServiceMappingBeans())
//		.thenReturn(new ArrayList<>());
//		Mockito.when(mockApplicationHealthBL.getOpenProblems(Mockito.anyInt(), Mockito.anyList(), Mockito.anyList()))
//		.thenReturn(appHealthData);
//		Assert.assertEquals("7640123a-fbde-4fe5-9812-581cd1e3a9c1",
//				applicationHealthBL.process(userAccessDetails));
//	}

}
