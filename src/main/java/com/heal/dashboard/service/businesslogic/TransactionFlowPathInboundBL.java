package com.heal.dashboard.service.businesslogic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.TFPRequestData;
import com.heal.dashboard.service.beans.TagDetails;
import com.heal.dashboard.service.beans.TagMapping;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.tpf.TFPServiceDetails;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.TagsDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.UserValidationUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TransactionFlowPathInboundBL implements BusinessLogic<Map, TFPRequestData, List<TFPServiceDetails>> {

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
		Map<String, String> queryParam = new HashMap<String,String>();
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

			List<Controller> mappedServices = getServicesMappedToApplication(allAccountTagList, allServiceList,
					controllerTagDetails.getId(), appId);
			log.debug("Found {} service mapped to applicationId: {}", mappedServices.size(), appId);

			List<Controller> entryPointServiceList = filterEntrypointServices(allAccountTagList, mappedServices,
					entrypointTagDetails.getId());

			return tfpCommonBL.getServiceTransactionStats(account, configData.getApplication(), null, null,
					configData.getUserAccessDetails(), entryPointServiceList, configData.getFromTime(),
					configData.getToTime());
		} catch (Exception ex) {
			throw new DataProcessingException("Exception occur in TransactionFlowPathInboundBL class  " + ex.getMessage());

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

}
