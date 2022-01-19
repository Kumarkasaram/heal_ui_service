package com.heal.dashboard.service.businesslogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.appnomic.appsone.api.common.UIMessages;
import com.appnomic.appsone.api.custom.exceptions.CassandraException;
import com.appnomic.appsone.api.service.mysql.MasterDataService;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.ApplicationSDMRequestBean;
import com.heal.dashboard.service.beans.CategoryDetailBean;
import com.heal.dashboard.service.beans.CategoryEvents;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.ViewTypeBean;
import com.heal.dashboard.service.config.CassandraConnectionManager;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.AnomalyDaoImpl;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.UserValidationUtil;

@Service
public class ApplicationSDMCategoryEventServiceBL
		implements BusinessLogic<Map, List<ApplicationSDMRequestBean>, List<CategoryEvents>> {

	@Autowired
	AccountDao accountDao;

	@Autowired
	UserValidationUtil userValidationUtil;

	@Autowired
	ControllerDao controllerDao;

	@Autowired
	MasterDataDao masterDataDao;
	
	@Autowired
    AnomalyDaoImpl anomalyDao;
	
	private static final String VOLUME_STRING = "Volume";
    private static final String IS_WORKLOAD_COLUMN = "is_workload";
    private static final String ANOMALIES_CASSANDRA_STRING = "anomalies";
    private static final String SERVICE_TRANSACTIONS_TABLE = "service_transactions_data";
    private static final String COLUMN_SERVICE_ID = "service_id";
    private static final String COLUMN_AGG_LEVEL = "agg_level";
    private static final String COLUMN_ACCOUNT_ID = "account_id";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_RESPONSE_TYPE = "response_type";

	@Override
	public UtilityBean<Map> clientValidation(Object requestBody, String... requestParams) throws ClientException {
		String jwtToken = requestParams[0];
		if (null == jwtToken || jwtToken.trim().isEmpty()) {
			throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
		}

		String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
		String identifier = requestParams[1];
		if (null == userId || userId.trim().isEmpty()) {
			throw new ClientException("User details extraction failure");
		}
		Map<String, String> map = new HashMap<>();
		map.put("serviceId", requestParams[3]);
		map.put("load", requestParams[2]);
		map.put("toTime", requestParams[5]);
		map.put("fromTime", requestParams[4]);

		return UtilityBean.<Map<String, String>>builder().accountIndentifier(identifier).authToken(userId)
				.pojoObject(map).build();
	}

	@Override
	public List<ApplicationSDMRequestBean> serverValidation(UtilityBean<Map> utilityBean) throws ServerException {
		AccountBean account = accountDao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
		if (account == null) {
			log.error("Error while fetching account details for identifier [{}]", utilityBean.getAccountIdentifier());
			throw new ServerException(
					"Error while fetching account details for identifier [{}]" + utilityBean.getAccountIdentifier());
		}

		UserAccessDetails userAccessDetails = userValidationUtil.getUserAccessDetails(utilityBean.getAuthToken(),
				utilityBean.getAccountIdentifier());
		if (userAccessDetails == null) {
			log.error("Exception occurred while fetching user access details for userId [{}] and account [{}]",
					utilityBean.getAuthToken(), utilityBean.getAccountIdentifier());
			throw new ServerException("Error while fetching user access details");
		}
		Controller controller = getControllerDetails(utilityBean.getPojoObject().get("applicationId"), account);
		checkAccessDetails(userAccessDetails, controller);
		validateTime(utilityBean);
		return ApplicationSDMRequestBean.builder().account(account).userAccessDetails(userAccessDetails)
				.controller(controller).fromTime(Long.parseLong(utilityBean.getPojoObject().get("FromTimeString")))
				.toTime(Long.parseLong(utilityBean.getPojoObject().get("ToTimeString"))).build();
	}

	private void checkAccessDetails(UserAccessDetails accessDetails, Controller controller) throws ServerException {
		if (!accessDetails.getApplicationIdentifiers().contains(controller.getIdentifier())) {
			throw new ServerException("Request Exception : User does not have access to this application.");
		}
	}

	private void validateTime(UtilityBean<Map<String, String>> utilityBean) throws ServerException {
		long fromTime = Long.parseLong(utilityBean.getPojoObject().get("FromTimeString"));
		long toTime = Long.parseLong(utilityBean.getPojoObject().get("ToTimeString").trim());
		if (toTime < fromTime) {
			// log.error("fromTime [{}] and toTime [{}] is invalid", fromTime, toTime);
			throw new ServerException("Request Exception : fromTime / toTime Invalid.");
		}
	}

	private Controller getControllerDetails(String appIdString, AccountBean account) throws ServerException {
		List<Controller> filtratedControllerList = new ArrayList<>();
		Controller controller = null;
		try {
			List<Controller> controllerList = controllerDao.getControllerList(account.getAccountId());
			ViewTypeBean subTypeBean = masterDataDao.getTypeInfoFromSubTypeName(Constants.CONTROLLER_TYPE_NAME_DEFAULT,
					Constants.APPLICATION_CONTROLLER_TYPE);
			filtratedControllerList = controllerList.stream()
					.filter(t -> t.getControllerTypeId() == subTypeBean.getSubTypeId() && t.getStatus() == 1)
					.collect(Collectors.toList());
			controller = filtratedControllerList.parallelStream()
					.filter(c -> Integer.parseInt(c.getAppId()) == Integer.parseInt(appIdString)).findAny()
					.orElse(null);

			if (controller == null) {
				log.error("Controller(Application/Service) [{}] invalid.", appIdString);
				throw new ServerException("Request Exception : Controller(Application/Service) invalid.");
			}
		} catch (Exception e) {
			log.error("Error occurred while fetching controller details for service name: "
					+ Constants.CONTROLLER_TYPE_NAME_DEFAULT + ", account id: " + account.getAccountId(), e);
		}
		return controller;
	}

	@Override
	public List<CategoryEvents> process(List<ApplicationSDMRequestBean> bean) throws DataProcessingException {
		List<Row> anomaliesForService = getAnomaliesForService(request.getAccount().getIdentifier(),
				request.getController().getIdentifier(), request.getFromTime(), request.getToTime());

		Map<String, CategoryDetailBean> category = Objects
				.requireNonNull(masterDataDao.getCategoryDetails(request.getAccount().getAccountId()))
				.parallelStream().collect(Collectors.toMap(CategoryDetailBean::getIdentifier, c -> c));

		Map<String, CategoryEvents> resultMap = new HashMap<>();

		Set<String> anomalyIds = new HashSet<>();
		anomaliesForService.forEach(row -> anomalyIds.addAll(row.getSet(ANOMALIES_CASSANDRA_STRING, String.class)));

		List<Row> anomalies = getAnomaliesFromIds(anomalyIds);
		anomalies.stream().filter(anomaly -> !anomaly.getBool(IS_WORKLOAD_COLUMN))
				.map(anomaly -> anomaly.getString("category_id")).forEach(categoryIdentifier -> {
					if (resultMap.containsKey(categoryIdentifier)) {
						resultMap.get(categoryIdentifier)
								.setEventCount(resultMap.get(categoryIdentifier).getEventCount() + 1);
					} else
						resultMap.put(categoryIdentifier,
								CategoryEvents.builder().categoryId(category.get(categoryIdentifier).getCategoryId())
										.categoryName(category.get(categoryIdentifier).getName()).eventCount(1)
										.build());
				});
		return new ArrayList<>(resultMap.values());
	}

	private static List<Row> getAnomaliesFromIds(Set<String> ids) {
		Session session = CassandraConnectionManager.getSession();
		if (session == null)
			throw new ServerException("Server Exception");

		return session.execute(
				QueryBuilder.select().all().from(ANOMALIES_CASSANDRA_STRING).where(QueryBuilder.in("anomaly_id", ids)))
				.all();
	}

	private List<Row> getAnomaliesForService(String accountIdentifier, String serviceIdentifier, long fromTime,
			long toTime) {
		String all = Constants.CASSANDRA_ALL_IDENTIFIER;
		return anomalyDao.getAnomalyEnrichDetails(accountIdentifier, serviceIdentifier, all, all, all, all, fromTime,
				toTime);
		/*
		 * Session session = CassandraConnectionManager.getSession();
		 * 
		 * if (session == null) throw new
		 * CassandraException(UIMessages.ERROR_DB_CASSANDRA_CONNECTION);
		 * 
		 * return session.execute(QueryBuilder.select().all()
		 * .from("anomalies_enrich_details") .where(QueryBuilder.eq(COLUMN_ACCOUNT_ID,
		 * accountIdentifier)).and( QueryBuilder.eq("controller_id",
		 * serviceIdentifier)).and( QueryBuilder.eq("instance_id",
		 * Constants.CASSANDRA_ALL_IDENTIFIER)).and( QueryBuilder.eq("category_id",
		 * Constants.CASSANDRA_ALL_IDENTIFIER)).and( QueryBuilder.eq("kpi_id",
		 * Constants.CASSANDRA_ALL_IDENTIFIER)).and( QueryBuilder.eq("kpi_attribute",
		 * Constants.CASSANDRA_ALL_IDENTIFIER)).and( QueryBuilder.lte("anomaly_time",
		 * toTime)).and( QueryBuilder.gte("anomaly_time", fromTime))).all();
		 */

	}

}
