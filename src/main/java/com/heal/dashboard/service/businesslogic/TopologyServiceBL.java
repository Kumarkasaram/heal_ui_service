package com.heal.dashboard.service.businesslogic;

import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.ControllerBean;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.Edges;
import com.heal.dashboard.service.beans.Nodes;
import com.heal.dashboard.service.beans.TopologyDetails;
import com.heal.dashboard.service.beans.TopologyValidationResponseBean;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;

@Slf4j
@Component
public class TopologyServiceBL implements BusinessLogic<String, TopologyValidationResponseBean, TopologyDetails> {

	@Autowired
	AccountDao accountDao;
	@Autowired
	ControllerDao controllerDao;
	@Autowired
	TopologyUtility topologyUtility;
	@Autowired
	private UserValidationUtil userValidationUtil;

	@Override
	public UtilityBean<String> clientValidation(Object requestBody, String... requestParams) throws ClientException {
		String jwtToken = requestParams[0];
		if (null == jwtToken || jwtToken.trim().isEmpty()) {
			throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
		}
		String identifier = requestParams[1];
		if (null == identifier || identifier.trim().isEmpty()) {
			throw new ClientException("Account identifier is invalid");
		}

		String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
		if (null == userId || userId.trim().isEmpty()) {
			throw new ClientException("User details extraction failure");
		}

		String applicationId = requestParams[2];
		if(applicationId == null || applicationId.trim().isEmpty()) {
			throw new ClientException("Query parameter applicationId is invalid");
		}

		return UtilityBean.<String>builder().authToken(userId).accountIdentifier(identifier)
				.pojoObject(applicationId).build();
	}

	@Override
	public TopologyValidationResponseBean serverValidation(UtilityBean<String> utilityBean) throws ServerException {
		List<Nodes> nodeList;
		List<Edges> edgesList;
		String applicationIdString = utilityBean.getPojoObject();
		String accountIdentifier = utilityBean.getAccountIdentifier();

		AccountBean account = accountDao.getAccountDetailsForIdentifier(accountIdentifier);
		if (account == null) {
			log.error("Error while fetching account details for identifier [{}]", utilityBean.getAccountIdentifier());
			throw new ServerException("Error while fetching account details for identifier [{}]" + utilityBean.getAccountIdentifier());
		}

		UserAccessDetails userAccessDetails = userValidationUtil.getUserAccessDetails(utilityBean.getAuthToken(), accountIdentifier);
		if (userAccessDetails == null) {
			log.error("Exception occurred while fetching user access details for userId [{}] and account [{}]", utilityBean.getAuthToken(), accountIdentifier);
			throw new ServerException("Error while fetching user access details");
		}

		Timestamp date;
		try {
			date = new Timestamp(DateUtil.getDateInGMT(System.currentTimeMillis()).getTime());
		} catch (ParseException e) {
			log.error("Exception occurred while fetching current time in GMT timezone. Details: ", e);
			throw new ServerException("Error while fetching current time in GMT timezone");
		}

		if (applicationIdString == null) {
			List<ControllerBean> serviceList = controllerDao.getAllServicesForAccount(account.getId());
			nodeList = topologyUtility.getNodeList(account, userAccessDetails, serviceList, date.getTime());
			edgesList = topologyUtility.getEdgeList(account.getId(), serviceList);
		} else {
			int applicationId = Integer.parseInt(applicationIdString);
			List<ControllerBean> serviceList = controllerDao.getServicesByAppId(applicationId, account.getId());
			nodeList = topologyUtility.getNodeList(account, userAccessDetails, serviceList, date.getTime());
			edgesList = topologyUtility.getEdgeList(account.getId(), serviceList);
		}

		return new TopologyValidationResponseBean(nodeList, edgesList, null);
	}

	@Override
	public TopologyDetails process(TopologyValidationResponseBean topologyValidationResponseBean) throws DataProcessingException {
		TopologyDetails topologyDetails = new TopologyDetails();
		topologyDetails.setNodes(topologyValidationResponseBean.getNodeList());
		topologyDetails.setEdges(topologyValidationResponseBean.getEdgesList());
		return topologyDetails;
	}
}
