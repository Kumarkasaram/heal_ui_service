package com.heal.dashboard.service.businesslogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.ApplicationSDMBean;
import com.heal.dashboard.service.beans.CompInstClusterDetails;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.ControllerBean;
import com.heal.dashboard.service.beans.Edges;
import com.heal.dashboard.service.beans.Nodes;
import com.heal.dashboard.service.beans.TagMapping;
import com.heal.dashboard.service.beans.TopologyDetails;
import com.heal.dashboard.service.beans.UserAccessBean;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.ViewTypeBean;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ComponentInstanceDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.dao.mysql.TagsDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.exception.UiServiceException;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.TopologyUtility;
import com.heal.dashboard.service.util.UserValidationUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ApplicationSDMBL implements BusinessLogic<String, ApplicationSDMBean, TopologyDetails> {

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private MasterDataDao masterDataDao;

	@Autowired
	private ControllerDao controllerDao;
	@Autowired
	UserValidationUtil userValidator;
	@Autowired
	TopologyUtility topologyUtility;
	@Autowired
	TagsDao tagDao;
	@Autowired
	ComponentInstanceDao componentInstanceDao;

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

		String fromTimeString = requestParams[2];
		if (null == fromTimeString || fromTimeString.trim().isEmpty()) {
			throw new ClientException("fromTime or toTime is null or empty.");
		}
		String toTimeString = requestParams[2];
		if (null == toTimeString || toTimeString.trim().isEmpty()) {
			throw new ClientException("fromTime or toTime is null or empty.");
		}

		String applicationId = requestParams[3];
		if (applicationId == null || applicationId.trim().isEmpty()) {
			throw new ClientException("Query parameter applicationId is invalid");
		}

		String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
		if (null == userId || userId.trim().isEmpty()) {
			log.error("Invalid JWT. User identifier extraction failed.");
			throw new ClientException("User details extraction failure");
		}

		return UtilityBean.<String>builder().authToken(jwtToken).accountIdentifier(identifier)
				.pojoObject(fromTimeString + "_" + toTimeString + "_" + applicationId).build();
	}

	@Override
	public ApplicationSDMBean serverValidation(UtilityBean<String> utilityBean) throws ServerException {
		AccountBean accountBean = accountDao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
		if (accountBean == null) {
			log.error("Invalid account identifier. Details: [{}] is unavailable", utilityBean.getAccountIdentifier());
			throw new ServerException("Invalid account identifier");
		}

		UserAccessBean userAccessBean = accountDao.fetchUserAccessDetailsUsingIdentifier(utilityBean.getAuthToken());
		if (null == userAccessBean || null == userAccessBean.getAccessDetails()) {
			log.error("Invalid user access details. Details: Required access details for user [{}] is unavailable",
					utilityBean.getAuthToken());
			throw new ServerException("Invalid user access details");
		}
		UserAccessDetails userAccessDetails = userValidator.getUserAccessDetails(userAccessBean.getUserIdentifier(),
				accountBean.getIdentifier());
		Controller controller = getControllerDetails(utilityBean.getPojoObject().split("_")[2], accountBean);
		checkAccessDetails(userAccessBean, controller);
		validateTime(utilityBean);

		return ApplicationSDMBean.builder().accountBean(accountBean).controller(controller)
				.userAccessDetails(userAccessDetails).fromTime(0).toTime(1).build();
	}

	@Override
	public TopologyDetails process(ApplicationSDMBean applicationSDMRequestBean) throws DataProcessingException {
		TopologyDetails topologyDetails = new TopologyDetails();

		Map<Integer, Controller> applications = null;
		int accountId = applicationSDMRequestBean.getAccountBean().getAccountId();
		int appId = Integer.parseInt(applicationSDMRequestBean.getController().getAppId());
		List<ControllerBean> serviceList = null;
		try {
			serviceList = controllerDao.getServicesByAppId(appId, accountId);

			List<Nodes> nodesList = topologyUtility.getNodeList(applicationSDMRequestBean.getAccountBean(),
					applicationSDMRequestBean.getUserAccessDetails(), serviceList,
					applicationSDMRequestBean.getToTime());
			List<Edges> edgesList = topologyUtility.getEdgeList(accountId, serviceList);
			List<String> controllerIds = nodesList.parallelStream().map(Nodes::getId).collect(Collectors.toList());

			int tagId = tagDao.getTagDetails(Constants.CONTROLLER_TAG, Constants.DEFAULT_ACCOUNT_ID).getId();
			List<TagMapping> controllerTagMappingDetails = tagDao.getTagMappingDetailsWithoutObjectId(tagId,
					Constants.CONTROLLER, accountId);

			List<ViewTypeBean> subTypeBeans = masterDataDao.getAllViewTypes();
			Optional<ViewTypeBean> viewTypeBean = subTypeBeans.stream()
					.filter(it -> (Constants.CONTROLLER_TYPE_NAME_DEFAULT.trim().equalsIgnoreCase(it.getTypeName())))
					.filter(it -> (Constants.APPLICATION_CONTROLLER_TYPE.trim().equalsIgnoreCase(it.getSubTypeName())))
					.findAny();
			if (viewTypeBean.isPresent()) {
				int appTypeId = viewTypeBean.get().getSubTypeId();
				applications = controllerDao.getApplicationList(accountId).parallelStream()
						.filter(c -> c.getControllerTypeId() == appTypeId)
						.collect(Collectors.toMap(c -> Integer.parseInt(c.getAppId()), c -> c));
			}

			Set<Edges> newEdges = new HashSet<>();

			for (Edges e : edgesList) {
				List<Nodes> nodes = new ArrayList<>();
				if (!controllerIds.contains(e.getSource())) {
					nodes = getControllerNode(applicationSDMRequestBean.getAccountBean(),
							applicationSDMRequestBean.getUserAccessDetails(), e.getSource(),
							controllerTagMappingDetails, applications, applicationSDMRequestBean.getToTime());
					for (Nodes n : nodes) {
						Edges secondEdge = Edges.clone(e);
						secondEdge.setSource(n.getId());
						newEdges.add(secondEdge);
					}
				} else if (!controllerIds.contains(e.getTarget())) {
					nodes = getControllerNode(applicationSDMRequestBean.getAccountBean(),
							applicationSDMRequestBean.getUserAccessDetails(), e.getTarget(),
							controllerTagMappingDetails, applications, applicationSDMRequestBean.getToTime());
					for (Nodes n : nodes) {
						Edges secondEdge = Edges.clone(e);
						secondEdge.setTarget(n.getId());
						newEdges.add(secondEdge);
					}
				} else {
					newEdges.add(e);
				}

				if (!nodes.isEmpty()) {
					nodes.forEach(n -> {
						if (!nodesList.contains(n)) {
							nodesList.add(n);
						}
					});

					nodes.forEach(n -> {
						if (!controllerIds.contains(n.getId())) {
							controllerIds.add(n.getId());
						}
					});
				}
			}

			topologyDetails.setNodes(updateFlags(nodesList, applicationSDMRequestBean));
			topologyDetails.setEdges(new ArrayList<>(newEdges));
			return topologyDetails;
		} catch (ServerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return topologyDetails;
	}

	private Controller getControllerDetails(String appIdString, AccountBean account) throws ServerException {
		Controller controller = getControllersByTypeBypassCache(Constants.APPLICATION_CONTROLLER_TYPE,
				account.getAccountId()).parallelStream()
						.filter(c -> Integer.parseInt(c.getAppId()) == Integer.parseInt(appIdString)).findAny()
						.orElse(null);

		if (controller == null) {
			log.error("Controller(Application/Service) [{}] invalid.", appIdString);
			throw new ServerException("Request Exception : Controller(Application/Service) invalid.");
		}
		return controller;
	}

	public List<Controller> getControllersByTypeBypassCache(String applicationControllerType, int accountId) {
		List<Controller> filtratedControllerList = new ArrayList<>();
		try {
			// get the service mst sub type
			List<ViewTypeBean> subTypeBeans = masterDataDao.getAllViewTypes();
			Optional<ViewTypeBean> viewTypeBean = subTypeBeans.stream()
					.filter(it -> (Constants.CONTROLLER_TYPE_NAME_DEFAULT.trim().equalsIgnoreCase(it.getTypeName())))
					.filter(it -> (applicationControllerType.trim().equalsIgnoreCase(it.getSubTypeName()))).findAny();

			// get the all app for accountId by passing cache
			List<Controller> controllerList = controllerDao.getControllerList(accountId);

			// filter with controller_type_id
			filtratedControllerList = controllerList.stream()
					.filter(t -> t.getControllerTypeId() == viewTypeBean.get().getSubTypeId() && t.getStatus() == 1)
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error occurred while fetching controller details for service name: " + applicationControllerType
					+ ", account id: " + accountId, e);
		}
		return filtratedControllerList;
	}

	private void checkAccessDetails(UserAccessBean userAccessDetails, Controller controller) throws ServerException {
		if (!userAccessDetails.getUserIdentifier().contains(controller.getIdentifier())) {
			throw new ServerException("Request Exception : User does not have access to this application.");
		}
	}

	private void validateTime(UtilityBean<String> utilityBean) throws ServerException {
		long fromTime = Long.parseLong(utilityBean.getPojoObject().split("_")[0]);
		long toTime = Long.parseLong(utilityBean.getPojoObject().split("_")[1]);
		if (toTime < fromTime) {
			log.error("fromTime [{}] and toTime [{}] is invalid", fromTime, toTime);
			throw new ServerException("Request Exception : fromTime / toTime Invalid.");
		}
	}

	private List<Nodes> updateFlags(List<Nodes> nodesList, ApplicationSDMBean request) {

		nodesList.parallelStream().filter(n -> !n.getType().equals("application")).forEach(n -> {

			int instanceCount = componentInstanceDao.getClustersForService(Integer.parseInt(n.getId())).size();
			int txnCount = 0;
			try {
				txnCount = tagDao.getTxnCountPerService(Integer.parseInt(n.getId()),
						request.getAccountBean().getAccountId());
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UiServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Map<String, Object> instances = fetchInstanceCount(request.getAccountBean(), n.getId());
			n.getMetadata().putAll(instances);

			if (txnCount > 0)
				n.setWorkloadEventCount(0);
			if (instanceCount > 0)
				n.setBehaviorEventCount(0);
		});

		/* nodesList = getEventStatus(nodesList,request); */
		return nodesList;
	}

	private List<Nodes> getControllerNode(AccountBean account, UserAccessDetails userAccessDetails, String id,
			List<TagMapping> tagMappingDetails, Map<Integer, Controller> applications, long toTime)
			throws ServerException {
		List<Controller> controllerList = controllerDao.getApplicationList(account.getAccountId());
		List<TagMapping> tagMappingDetailsList = tagMappingDetails.parallelStream()
				.filter(t -> t.getTagKey().equals(id)).collect(Collectors.toList());
		Controller cntrl = controllerList.parallelStream().filter(c -> c.getAppId().equals(id)).findAny().orElse(null);

		ControllerBean controllerBean = (cntrl != null)
				? ControllerBean.builder().id(Integer.parseInt(cntrl.getAppId())).name(cntrl.getName())
						.identifier(cntrl.getIdentifier()).build()
				: null;

		if (tagMappingDetailsList.isEmpty()) {
			Nodes node = topologyUtility.getNode(controllerBean, tagMappingDetails, new ArrayList<>(), toTime);
			return Collections.singletonList(node);
		}

		boolean isAccessible = false;

		List<Nodes> outputNodes = new ArrayList<>();

		for (TagMapping tagMappingDetail : tagMappingDetailsList) {
			Controller controller = applications.get(tagMappingDetail.getObjectId());

			List<String> apps = userAccessDetails.getApplicationIdentifiers();

			if (apps.contains(controller.getIdentifier())) {
				isAccessible = true;
			}
			if (controller != null) {

				outputNodes.add(Nodes.builder().id(controller.getAppId()).name(controller.getName())
						.identifier(controller.getIdentifier()).type("application").userAccessible(isAccessible)
						.build());
			}
		}

		return outputNodes;
	}

	public Map<String, Object> fetchInstanceCount(AccountBean account, String appId) {
		Map<String, Object> metaData = new HashMap<>();

		List<CompInstClusterDetails> mappedClusterList = componentInstanceDao
				.getClusterListForService(account.getAccountId(), Integer.parseInt(appId));
		ArrayList<String> hostInstances = new ArrayList<>();
		ArrayList<String> componentInstances = new ArrayList<>();
		long start = System.currentTimeMillis();
		for (CompInstClusterDetails cluster : mappedClusterList) {

			// TODO : Need to use IN query and remove the db call inside this loop(Didn't do
			// it as we have some issues with JDBI in Prod)
			List<CompInstClusterDetails> allInstancesOfCluster1 = componentInstanceDao
					.getinstancesForCluster(cluster.getInstanceId());// MasterCache.getInstance().getInstancesForCluster(clusterId);

			for (CompInstClusterDetails instanceDetail : allInstancesOfCluster1) {
				if (instanceDetail.getHostId() == 0) {
					if (!hostInstances.contains(instanceDetail.getInstanceName()))
						hostInstances.add(instanceDetail.getInstanceName());
				} else if (!componentInstances.contains(instanceDetail.getInstanceName()))
					componentInstances.add(instanceDetail.getInstanceName());
			}
		}
		metaData.put("hostInstances", hostInstances.size());
		metaData.put("componentInstances", componentInstances.size());
		log.debug("Component Instances : {} & Host Instances : {} for serviceId : {}", componentInstances.toString(),
				hostInstances.toString(), appId);
		log.debug("Time taken to fetching the instances for a given service {} : {}  ", appId,
				System.currentTimeMillis() - start);

		return metaData;
	}

}
