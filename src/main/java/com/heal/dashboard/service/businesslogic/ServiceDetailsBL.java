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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ServiceDetailsBL implements BusinessLogic<String, TopologyValidationResponseBean, TopologyDetails> {
	@Autowired
	AccountDao accountDao;
	@Autowired
	ControllerDao controllerDao;
	@Autowired
	UserValidationUtil userValidationUtil;
	@Autowired
	TopologyUtility topologyUtility;
	String accountIdentifier;
	String serviceId;
	String nDegree;

	@Override
	public UtilityBean<String> clientValidation(Object requestBody, String... requestParams) throws ClientException {
		String jwtToken = requestParams[0];
		if (null == jwtToken || jwtToken.trim().isEmpty()) {
			throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
		}

		accountIdentifier = requestParams[1];
		if (null == accountIdentifier || accountIdentifier.trim().isEmpty()) {
			throw new ClientException("identifier cant be null or empty");
		}

		serviceId = requestParams[2];
		if (null == serviceId || serviceId.trim().isEmpty()) {
			throw new ClientException("serviceId cant be null or empty");
		}

		nDegree = requestParams[3];
		if (null == nDegree || nDegree.trim().isEmpty()) {
			throw new ClientException("nDegree cant be null or empty");
		}

		String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
		if (null == userId || userId.trim().isEmpty()) {
			throw new ClientException("User details extraction failure");
		}
		
		return UtilityBean.<String>builder()
				.authToken(userId)
				.accountIdentifier(accountIdentifier)
				.build();
	}

	@Override
	public TopologyValidationResponseBean serverValidation(UtilityBean<String> utilityBean) throws ServerException {
		AccountBean account = accountDao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());

		if (account == null) {
			log.error("Account details unavailable for identifier [{}]", utilityBean.getAccountIdentifier());
			throw new ServerException("Account details unavailable");
		}

		UserAccessDetails userAccessDetails = userValidationUtil.getUserAccessDetails(utilityBean.getAuthToken(), utilityBean.getAccountIdentifier());
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

		List<ControllerBean> serviceList = controllerDao.getAllServicesForAccount(account.getId());

		List<Nodes> nodeList = topologyUtility.getNodeList(account, userAccessDetails, serviceList, date.getTime());
		List<Edges> edgesList = topologyUtility.getEdgeList(account.getAccountId(), serviceList);

		return new TopologyValidationResponseBean(nodeList, edgesList, serviceId);
	}

	@Override
	public TopologyDetails process(TopologyValidationResponseBean topologyValidationResponseBean) throws DataProcessingException {
        TopologyDetails topologyDetails = new TopologyDetails();
        String serviceIdStr = topologyValidationResponseBean.getServiceId();

        List<Edges> sourceNodeList = topologyValidationResponseBean.getEdgesList().parallelStream()
                .filter(t -> t.getTarget().equals(serviceIdStr)).collect(Collectors.toList());

        List<Edges> destinationList = topologyValidationResponseBean.getEdgesList().parallelStream()
                .filter(t -> t.getSource().equals(serviceIdStr)).collect(Collectors.toList());

        List<Edges> filterEdges = new ArrayList<>();
        filterEdges.addAll(sourceNodeList);
        filterEdges.addAll(destinationList);

        Set<Nodes> filterNodeList = new HashSet<>();

        for (Edges edges : filterEdges) {
            String source = edges.getSource();
            Optional<Nodes> sNodes = topologyValidationResponseBean.getNodeList().parallelStream()
                    .filter(t -> t.getId().equalsIgnoreCase(source))
                    .findAny();
            String destination = edges.getTarget();
            Optional<Nodes> dNodes = topologyValidationResponseBean.getNodeList().parallelStream()
                    .filter(t -> t.getId().equalsIgnoreCase(destination))
                    .findAny();

            sNodes.ifPresent(filterNodeList::add);
            dNodes.ifPresent(filterNodeList::add);
        }

        Nodes selectedNode = new Nodes();

        // select node populating.
        Optional<Nodes> selectNode =topologyValidationResponseBean.getNodeList().parallelStream()
                .filter(t -> t.getId().equals(serviceIdStr)).findAny();
        if (selectNode.isPresent()) {
            selectedNode.setId(serviceIdStr);
            selectedNode.setName(selectNode.get().getName());
            selectedNode.setType(selectNode.get().getType());
        }

        filterNodeList.add(selectedNode);
        topologyDetails.setNodes(new ArrayList<>(filterNodeList));
        topologyDetails.setEdges(filterEdges);
		return topologyDetails;
	}
}
