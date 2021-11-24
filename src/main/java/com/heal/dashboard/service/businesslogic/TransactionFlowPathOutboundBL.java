package com.heal.dashboard.service.businesslogic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.ConnectionDetails;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.TFPRequestData;
import com.heal.dashboard.service.beans.TagDetails;
import com.heal.dashboard.service.beans.TagMapping;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.ViewTypeBean;
import com.heal.dashboard.service.beans.tpf.TFPServiceDetails;
import com.heal.dashboard.service.beans.tpf.TransactionDirection;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.dao.mysql.TagsDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CommonServiceBLUtil;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.UserValidationUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TransactionFlowPathOutboundBL implements BusinessLogic<Map, TFPRequestData, List<TFPServiceDetails>> {

	@Autowired
	private UserValidationUtil userValidationUtil;
	@Autowired
	private ControllerDao controllerDao;
	@Autowired
	private AccountDao accountDao;
	@Autowired
	private TagsDao tagDao;
	@Autowired
	TFPCommonBL tfpCommonBL;
	@Autowired
	MasterDataDao masterDao;
	@Autowired
	CommonServiceBLUtil commonServiceUtil;

	@Override
	public UtilityBean<Map> clientValidation(Object requestObject, String... requestParams) throws ClientException {
		String jwtToken = requestParams[0];
		if (null == jwtToken || jwtToken.trim().isEmpty()) {
			throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
		}
		String identifier = requestParams[1];
		if (null == identifier || identifier.trim().isEmpty()) {
			throw new ClientException("identifier cant be null or empty");
		}

		String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
		if (null == userId || userId.trim().isEmpty()) {
			throw new ClientException("User details extraction failure");
		}
		String applicationId = requestParams[2];
		if (null == applicationId || applicationId.trim().isEmpty()) {
			throw new ClientException("applicationId cant be null or empty");
		}
		String fromTime = requestParams[3];
		if (fromTime == null || fromTime.trim().isEmpty()) {
			throw new ClientException("Query parameter fromTime is invalid");
		}
		String toTime = requestParams[4];
		if (toTime == null || toTime.trim().isEmpty()) {
			throw new ClientException("Query parameter toTime is invalid");
		}
		Map<String, String> queryParam = new HashMap<String, String>();
		queryParam.put("fromTime", fromTime);
		queryParam.put("toTime", toTime);
		queryParam.put("applicationId", applicationId);
		return UtilityBean.<Map>builder().authToken(userId).accountIdentifier(identifier).pojoObject(queryParam)
				.build();
	}

	@Override
	public TFPRequestData serverValidation(UtilityBean<Map> utilityBean) throws ServerException {
		UserAccessDetails userAccessDetails = userValidationUtil.getUserAccessDetails(utilityBean.getAuthToken(),
				utilityBean.getAccountIdentifier());
		if (userAccessDetails == null) {
			log.error("User access details unavailable for user [{}]", utilityBean.getAuthToken());
			throw new ServerException("User access details unavailable");
		}
		AccountBean accountBean = accountDao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
		if (accountBean == null) {
			log.error("Invalid account identifier. Details: [{}] is unavailable", utilityBean.getAccountIdentifier());
			throw new ServerException("Invalid account identifier");
		}
		TFPRequestData tfpRequestData = new TFPRequestData();
		tfpRequestData.setUserAccessDetails(userAccessDetails);
		tfpRequestData.setFromTime((long) utilityBean.getPojoObject().get("fromTime"));
		tfpRequestData.setToTime((long) utilityBean.getPojoObject().get("toTime"));
		tfpRequestData.setAccount(accountBean);
		Controller application = controllerDao.getControllerListByAccountIdAndApplicationId(accountBean.getAccountId(),
				Integer.parseInt(utilityBean.getPojoObject().get("applicationId").toString()));
		tfpRequestData.setApplication(application);
		log.info("All parameter received have been validated from server.");
		return tfpRequestData;
	}

	@Override
	public List<TFPServiceDetails> process(TFPRequestData configData) throws DataProcessingException {

		try {
			AccountBean account = configData.getAccount();
			long start;
			int appId = Integer.parseInt(configData.getApplication().getAppId());
			start = System.currentTimeMillis();
			List<TagMapping> allAccountTagList = tagDao.getTagMappingDetailsByAccountId(account.getAccountId());
			TagDetails controllerTagDetails = tagDao.getTagDetails(Constants.CONTROLLER_TAG,
					Constants.DEFAULT_ACCOUNT_ID);
			TagDetails entrypointTagDetails = tagDao.getTagDetails(Constants.ENTRY_POINT, Constants.DEFAULT_ACCOUNT_ID);
			List<Controller> allServiceList = controllerDao.getControllerList(account.getAccountId());
			log.debug("Time taken to fetch configuration data is {} ms.", (System.currentTimeMillis() - start));
			List<ConnectionDetails> allConnectionList = masterDao.getConnectionDetails(account.getAccountId());
			if (allAccountTagList == null || allAccountTagList.isEmpty() || appId == 0 || controllerTagDetails == null
					|| entrypointTagDetails == null || allServiceList == null || allServiceList.isEmpty()) {
				log.error("Unable to get mapped services to the given accId: {}, appId: {}", account.getAccountId(),
						appId);
				throw new DataProcessingException("Unable to get mapped services");
			}
			// Filter out only controller tags for performance
			allAccountTagList = allAccountTagList.stream()
					.filter(it -> Constants.CONTROLLER.equalsIgnoreCase(it.getObjectRefTable()))
					.collect(Collectors.toList());
			List<Controller> allEntryPointServiceList = filterEntrypointServices(allAccountTagList, allServiceList,
					entrypointTagDetails.getId());
			
			List<Controller> mappedServices = getServicesMappedToApplication(allAccountTagList, allServiceList,
					controllerTagDetails.getId(), appId);
			log.debug("Found {} service mapped to applicationId: {}", mappedServices.size(), appId);

	        Map<String, List<Controller>> serviceApplicationMap = commonServiceUtil.getServiceApplicationMap(account.getAccountId());

			List<Controller> mappedServiceApplication = mappedServices.stream()
	                .filter( controller -> controller.getIdentifier().equals(serviceApplicationMap.get(controller.getIdentifier()).size()==1))
	                .collect(Collectors.toList());
			
	        List<Controller> entryPointServiceList = excludeApplicationEntrypointServices(mappedServiceApplication,allEntryPointServiceList);
			List<Controller> outboundServiceList = getAdjecentEntrypointServices(allConnectionList, mappedServices,
					entryPointServiceList);

			List<TFPServiceDetails> result = tfpCommonBL.getServiceTransactionStats(account,
					configData.getApplication(), mappedServices, TransactionDirection.OUTBOUND, configData.getUserAccessDetails(), outboundServiceList,
					configData.getFromTime(), configData.getToTime());

			enrichApplicationName(allAccountTagList, allServiceList, controllerTagDetails.getId(),
					configData.getUserAccessDetails(), result);
			return result;
		} catch (Exception ex) {
			throw new DataProcessingException(
					"Exception occur in TransactionFlowPathInboundBL class  " + ex.getMessage());
		}
	}

	
	public static List<Controller> getServicesMappedToApplication(List<TagMapping> accountTags,
		List<Controller> controllerList, int tagDetailsId, int appId) {

		// filter out tags of the application mapped to services.
		List<TagMapping> tagMappingDetails = accountTags.stream()
				.filter(tag -> tag.getTagId() == tagDetailsId && tag.getObjectId() == appId)
				.collect(Collectors.toList());

		// fetch data related to the mapped service.
		return controllerList.stream()
				.filter(svc -> tagMappingDetails.stream().anyMatch(tag -> tag.getTagKey().equals(svc.getAppId())))
				.collect(Collectors.toList());
	}

	public static List<Controller> filterEntrypointServices(List<TagMapping> tagList, List<Controller> serviceList,
			int tagDetailsId) {

		List<TagMapping> entrypointTagList = tagList.stream().filter(tag -> (tag.getTagId() == tagDetailsId))
				.collect(Collectors.toList());
		log.debug("Found {} entrypoint tags.", entrypointTagList.size());

		return serviceList.stream()
				.filter(svc -> entrypointTagList.stream()
						.anyMatch(tag -> tag.getObjectId() == Integer.parseInt(svc.getAppId())))
				.collect(Collectors.toList());

	}

	private List<Controller> getAdjecentEntrypointServices(List<ConnectionDetails> allConnectionDetails,
			List<Controller> mappedServiceList, List<Controller> entrypointServiceList) {
		allConnectionDetails = allConnectionDetails.stream()
				.filter(it -> Constants.CONTROLLER.equalsIgnoreCase(it.getSourceRefObject())
						&& Constants.CONTROLLER.equalsIgnoreCase(it.getDestinationRefObject()))
				.collect(Collectors.toList());

		// Get a list of all entrypoint service id where source is mapped svc and
		// destination is an entry point svc
		Set<Integer> filteredServiceId = allConnectionDetails.stream()
				.filter(it -> (entrypointServiceList.stream()
						.anyMatch(epsvc -> Integer.parseInt(epsvc.getAppId()) == it.getDestinationId())))
				.filter(it -> (mappedServiceList.stream()
						.anyMatch(svc -> Integer.parseInt(svc.getAppId()) == it.getSourceId())))
				.map(ConnectionDetails::getDestinationId).collect(Collectors.toSet());

		// return a list of entry point svc list where the source is a mapped service
		return entrypointServiceList.stream()
				.filter(epsvc -> filteredServiceId.contains(Integer.parseInt(epsvc.getAppId())))
				.collect(Collectors.toList());
	}

	private List<Controller> excludeApplicationEntrypointServices(List<Controller> mappedServiceList,
			List<Controller> allEntrypointServiceList) {
		return allEntrypointServiceList.stream().filter(
				allSvc -> (mappedServiceList.stream().noneMatch(it -> (it.getAppId().equals(allSvc.getAppId())))))
				.collect(Collectors.toList());
	}

	protected void enrichApplicationName(List<TagMapping> tagList, List<Controller> allSvcList, int controllerTagId,
			UserAccessDetails userAccessDetails, List<TFPServiceDetails> resultList) throws ServerException {
		long start = System.currentTimeMillis();
		Map<String, Controller> controllerMap =null;
		List<TagMapping> filteredList = tagList.stream().filter(it -> it.getTagId() == controllerTagId)
				.collect(Collectors.toList());
		
	        Optional<ViewTypeBean> subTypeOptional = masterDao.getAllViewTypes()       
	                .stream()
	                .filter(it -> (Constants.CONTROLLER_TYPE_NAME_DEFAULT.trim().equalsIgnoreCase(it.getTypeName())))
	                .filter(it -> (Constants.APPLICATION_CONTROLLER_TYPE.trim().equalsIgnoreCase(it.getSubTypeName())))
	                .findAny();
	        if(subTypeOptional.isPresent()) {
	   		ViewTypeBean applicationType = subTypeOptional.get();	        
	        controllerMap = allSvcList.stream()
				.filter(it -> it.getControllerTypeId() == applicationType.getSubTypeId())
				.collect(Collectors.toMap(Controller::getAppId, java.util.function.Function.identity()));
	        }
	        
	        for (TFPServiceDetails serviceDetails : resultList) {
			TagMapping appTag = filteredList.stream()
					.filter(it -> it.getTagKey().equalsIgnoreCase(Integer.toString(serviceDetails.getServiceId())))
					.findAny().orElse(null);

			if (appTag == null) {
				log.error("Unable to get application tag for service id: {}.", serviceDetails.getServiceId());
				continue;
			}

			Controller app = controllerMap.get(Integer.toString(appTag.getObjectId()));

			if (app == null) {
				log.error("Unable to get application details for app id: {}.", appTag.getObjectId());
				continue;
			}
			List<String> apps = userAccessDetails.getApplicationIdentifiers();

			if (apps.contains(app.getIdentifier()))
				serviceDetails.setUserAccess(true);
			else
				serviceDetails.setUserAccess(false);

			serviceDetails.setApplicationName(app.getName());
			log.debug("Enriched app name: {}.", serviceDetails);
		}
		log.debug("Time taken to enrich app name for outbound services is {} ms.",
				(System.currentTimeMillis() - start));
	}

}
