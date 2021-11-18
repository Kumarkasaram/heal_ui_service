package com.heal.dashboard.service.util;

import com.heal.dashboard.service.beans.*;
import com.heal.dashboard.service.businesslogic.MaintainanceWindowsBL;
import com.heal.dashboard.service.dao.mysql.AgentDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.dao.mysql.TagsDao;
import com.heal.dashboard.service.enums.ComponentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TopologyUtility {

	@Autowired
	ControllerDao controllerDao;
	@Autowired
	MasterDataDao masterDataDao;
	@Autowired
	AgentDao agentDao;
	@Autowired
	TagsDao tagsDao;
	@Autowired
	MaintainanceWindowsBL maintainanceWindowsBL;
	
	public List<Nodes> getNodeList(AccountBean account, UserAccessDetails userAccessDetails, List<ControllerBean> serviceList, long toTime) {
		try {
			List<String> accessibleServiceList = userAccessDetails.getServiceIdentifiers();

			List<Integer> jimAgentIds = null;
			ViewTypeBean viewType = masterDataDao.getTypeInfoFromSubTypeName(Constants.AGENT_TYPE.trim(), Constants.JIM_AGENT_TYPE.trim());
			if(viewType == null) {
				log.error("JIM Agent type information unavailable");
			} else {
				jimAgentIds = agentDao.getJimAgentIds(viewType.getSubTypeId());
			}

			List<Integer> finalJimAgentIds = jimAgentIds;
			return serviceList.parallelStream().map(s -> {
				boolean flag = false;

				List<String> applications = controllerDao.getApplicationNamesBySvcId(account.getId(), s.getId());
				List<TagMapping> serviceTags = getServiceTags(s.getId(), account.getId());

				if (accessibleServiceList.contains(s.getIdentifier())) {
					flag = true;
				}

				Nodes serviceNode = getNode(s, serviceTags, finalJimAgentIds, toTime);
				serviceNode.setApplicationName(applications);
				serviceNode.setUserAccessible(flag);

				return serviceNode;

			}).collect(Collectors.toList());
		} catch (Exception ex) {
			log.error("Error occurred while getting nodes for accountId : {}", account.getId(), ex);
		}

		return Collections.emptyList();
	}
	
	public List<Edges> getEdgeList(int accountId, List<ControllerBean> serviceList) {

		List<Edges> edgesList = new ArrayList<>();
		try {
			List<ConnectionDetails> connectionDetailsList = masterDataDao.getConnectionDetails(accountId);

			List<ConnectionDetails> filterConnectionDetails = connectionDetailsList.stream()
					.filter(t -> t.getSourceRefObject().equalsIgnoreCase(Constants.CONTROLLER_TAG))
					.filter(t -> t.getDestinationRefObject().equalsIgnoreCase(Constants.CONTROLLER_TAG))
					.filter(t -> t.getSourceId() > 0)
					.filter(t -> t.getDestinationId() > 0)
					.collect(Collectors.toList());

			for (ConnectionDetails connectionDetails : filterConnectionDetails) {
				Edges edges = new Edges();
				// checking for services only
				Optional<ControllerBean> controller = serviceList.stream()
						.filter(t -> t.getId() == connectionDetails.getSourceId()
								|| t.getId() == connectionDetails.getDestinationId())
						.findAny();

				// extra validation so that duplicate edges are not created
				if (controller.isPresent() && (edgesList.stream()
						.noneMatch(it -> (it.getSource().equals(String.valueOf(connectionDetails.getSourceId()))
								&& it.getTarget().equals(String.valueOf(connectionDetails.getDestinationId())))))) {
					edges.setSource(String.valueOf(connectionDetails.getSourceId()));
					edges.setTarget(String.valueOf(connectionDetails.getDestinationId()));
					edgesList.add(edges);
				}
			}
		} catch (Exception e) {
			log.error("Error occurred while getting Edges for accountId [{}]. Details: ", accountId, e);
		}
		return edgesList;
	}

	public Nodes getNode(ControllerBean service, List<TagMapping> tagList, List<Integer> jimAgentIds, long toTime) {
		Nodes serviceNode = new Nodes();
		
		serviceNode.setId(String.valueOf(service.getId()));
		serviceNode.setName(service.getName());
		serviceNode.setIdentifier(service.getIdentifier());
		serviceNode.setType(ComponentType.unknown.name());
		
		long start = System.currentTimeMillis();
		serviceNode.setMaintenance(maintainanceWindowsBL.getServiceMaintenanceStatus(service.getName(), new Timestamp(toTime)));
		log.trace("Time taken for maintenance window details is {} ms.", (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		TagDetails tagDetailsBean = tagsDao.getTagDetails(Constants.LAYER_TAG, Constants.DEFAULT_ACCOUNT_ID);
		if (tagDetailsBean == null) {
			log.error("Tag {} does not exist in database.", Constants.LAYER_TAG);
			return serviceNode;
		}

		TagMapping layerDetails = tagList.stream().filter(tag -> tag.getTagId() == tagDetailsBean.getId())
				.filter(it -> it.getObjectId() == service.getId()).findAny().orElse(null);

		//The field in tag may contain information about the title to be shown in the icon in UI, if that is the case
		// then there will be title as well as type in tag value which will be separated by a splitter , below we handle
		// that scenario
		if (layerDetails != null) {
			String[] splitType = layerDetails.getTagValue().toLowerCase().split(Constants.ICON_TITLE_SPLITTER_DEFAULT);
			if (splitType.length == 2) {
				serviceNode.setType(splitType[0]);
				serviceNode.setTitle(splitType[1]);
			} else {
				serviceNode.setType(layerDetails.getTagValue().toLowerCase());
			}
		}
		log.debug("Time taken for layer details details is {} ms.", (System.currentTimeMillis() - start));

		serviceNode.addToMetaData("jimEnabled", getJIMEnabledServiceId(jimAgentIds, tagList) ? 1 : 0);

		start = System.currentTimeMillis();
		List<TagMapping> entrypointSvcList = tagList.stream()
				.filter(it -> it.getObjectId() == service.getId())
				.filter(tag -> tag.getTagKey().equalsIgnoreCase(Constants.DEFAULT_ENTRY_POINT))
				.collect(Collectors.toList());

		serviceNode.setEntryPointNode(entrypointSvcList.size() > 0);

		log.trace("Time taken for entry point details is {} ms.", (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		List<TagMapping> kubernetesTagList = tagList.stream()
				.filter(tag -> tag.getTagValue().equalsIgnoreCase(Constants.KUBERNETES))
				.filter(it -> it.getObjectId() == service.getId()).collect(Collectors.toList());

		serviceNode.addToMetaData("isKubernetes", (kubernetesTagList.size() > 0) ? 1 : 0);

		log.trace("Time taken for Kubernetes details is {} ms.", (System.currentTimeMillis() - start));

		return serviceNode;
	}

	public boolean getJIMEnabledServiceId(List<Integer> jimAgentIds, List<TagMapping> tagList) {
		if (jimAgentIds.isEmpty() || tagList.isEmpty()) {
			log.debug("Agent list or tag list is empty. Checking action [JIM enabled] has failed.");
			return false;
		}

		List<TagMapping> serviceCtrlMappings = tagList.parallelStream()
				.filter(tag -> Constants.AGENT_TABLE.equalsIgnoreCase(tag.getObjectRefTable())
						&& jimAgentIds.contains(tag.getObjectId()))
				.collect(Collectors.toList());

		if (serviceCtrlMappings.size() > 0) {
			log.debug("JIM is enabled for serviceId [{}]", serviceCtrlMappings.get(0).getTagKey());
			return true;
		}

		return false;
	}

	public List<TagMapping> getServiceTags(int serviceId, int accountId) {
		long st = System.currentTimeMillis();

		TagDetails layerTag = tagsDao.getTagDetails(Constants.LAYER_TAG, Constants.DEFAULT_ACCOUNT_ID);
		if (layerTag == null) {
			log.error("Layer details unavailable for serviceId [{}] mapped to accountId [{}]", serviceId, accountId);
			return Collections.emptyList();
		}

		List<TagMapping> serviceTags = tagsDao.getTagMappingDetails(layerTag.getId(), serviceId, Constants.CONTROLLER, accountId);

		TagDetails entrypointTag = tagsDao.getTagDetails(Constants.ENTRY_POINT, Constants.DEFAULT_ACCOUNT_ID);
		if (entrypointTag == null) {
			log.error("Entry point details unavailable from tag details");
		} else {
			serviceTags.addAll(tagsDao.getTagMappingDetails(entrypointTag.getId(), serviceId, Constants.CONTROLLER, accountId));
		}

		TagDetails serviceTypeTag = tagsDao.getTagDetails(Constants.SERVICE_TYPE_TAG, Constants.DEFAULT_ACCOUNT_ID);
		if (serviceTypeTag == null) {
			log.error("Service type details unavailable from tag details");
		} else {
			serviceTags.addAll(tagsDao.getTagMappingDetails(serviceTypeTag.getId(), serviceId, Constants.CONTROLLER, accountId));
		}

		serviceTags.addAll(tagsDao.getTagMappingDetailsByTagKey(String.valueOf(serviceId), Constants.CONTROLLER, accountId));
		serviceTags.addAll(tagsDao.getTagMappingDetailsByTagKey(String.valueOf(serviceId), Constants.AGENT_TABLE, accountId));

		log.trace("Time take for getting service tags is {} ms. serviceId:{}, accountId:{}", (System.currentTimeMillis() - st), serviceId, accountId);

		return serviceTags;
	}
}
