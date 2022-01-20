package com.heal.dashboard.service.businesslogic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;

import com.datastax.driver.core.Row;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.SignalData;
import com.heal.dashboard.service.beans.SignalRequestPojo;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.AccountCassandraDao;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;

public class SignalDataServiceBL implements BusinessLogic<SignalRequestPojo, UtilityBean<SignalRequestPojo>, Set<SignalData>> {

	@Autowired
	AccountDao accountDao;
	@Autowired
	ControllerDao controllerDao;
	@Autowired
	AccountCassandraDao accountCassandraDao;
	
	@Override
	public UtilityBean<SignalRequestPojo> clientValidation(Object requestBody, String... requestParams)
			throws ClientException {
		
		Map<String, String> queryparam = (Map<String,String>) requestBody;
		
		String jwtToken = requestParams[0];
		String identifier = requestParams[1];
		
		String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
		if (null == userId || userId.trim().isEmpty()) {
			throw new ClientException("User details extraction failure");
		}

		
		String serviceIdString = queryparam.get("serviceId") != null ? queryparam.get("serviceId") : null;
		String signalIdString = queryparam.get("signalId") != null ? queryparam.get("signalId") : null;
		String status = queryparam.get("status") != null ? queryparam.get("status") : null;
		String fromTimeString = queryparam.get("fromTime") != null ? queryparam.get("fromTime") : null;
		String toTimeString = queryparam.get("toTime") != null ? queryparam.get("toTime") : null;

		Integer serviceId = null;
		Long fromTime;
		Long toTime;

		if (identifier.isEmpty()) {
			throw new ClientException("REQUEST_PARAM_IDENTIFIER" + identifier);
		}

		if (null != serviceIdString) {
			try {
				serviceId = Integer.parseInt(serviceIdString);
			} catch (NumberFormatException e) {
				throw new ClientException("Servicing Id " + e);
			}
		}

		try {
			fromTime = (fromTimeString == null) ? null : Long.parseLong(fromTimeString);
		} catch (NumberFormatException e) {
			throw new ClientException(
					"Error occurred while converting the fromTime [{0}]. Reason: {1}" + fromTimeString);
		}

		try {
			toTime = (toTimeString == null) ? null : Long.parseLong(toTimeString);
		} catch (NumberFormatException e) {
			throw new ClientException("Error occurred while converting the toTime [{0}]. Reason: {1}" + toTimeString);
		}

		if ((fromTime != null && toTime != null) && (fromTime <= 0 || toTime <= 0 || fromTime > toTime)) {
			log.error("TAG_INVALID_FROM_TO_TIME");
			throw new ClientException("TAG_INVALID_FROM_TO_TIME");
		}

		SignalRequestPojo pojo = new SignalRequestPojo();
		pojo.setActualFromTime(fromTime);
		fromTime = checkSignalWindowTime(fromTime, toTime);
		pojo.setSignalId(signalIdString);
		pojo.setFromTime(fromTime);
		pojo.setToTime(toTime);
		pojo.setStatus(status);

		return UtilityBean.<SignalRequestPojo>builder().accountIdString(identifier).serviceId(serviceId)
				.pojoObject(pojo).authToken(userId).build();
	}


	@Override
	public UtilityBean<SignalRequestPojo> serverValidation(UtilityBean<SignalRequestPojo> utilityBean)
			throws ServerException {
		AccountBean account = accountDao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
		if (account == null) {
			log.error("Error while fetching account details for identifier [{}]", utilityBean.getAccountIdentifier());
			throw new ServerException(
					"Error while fetching account details for identifier [{}]" + utilityBean.getAccountIdentifier());
		}
	        if(null!=utilityBean.getServiceId()) {
	        	 String serviceIdentifier = getServiceIdentifier(account.getAccountId(),Integer.parseInt(utilityBean.getServiceId()));
	             if ("".equals(serviceIdentifier)) {
	                 String err = Constants.MESSAGE_INVALID_SERVICE + " " + utilityBean.getPojoObject();
	                 log.error(err);
	                 throw new ServerException(Constants.MESSAGE_INVALID_SERVICE);
	             }
	        }

	        utilityBean.setAccount(account);
	        return utilityBean;
	}

	@Override
	public Set<SignalData> process(UtilityBean<SignalRequestPojo> bean) throws DataProcessingException {
		  SignalRequestPojo requestPojo = bean.getPojoObject();

	        Set<SignalData> responseData = new HashSet<>();
	        
	        Set<String> signalIds = new HashSet<>();
	        if (requestPojo.getSignalId() == null) {
	            signalIds.addAll(accountCassandraDao.getSignalId(bean.getAccountIdString(), requestPojo.getFromTime(), requestPojo.getToTime()));
	            log.debug("Signal Ids fetched from cassandra: {}", signalIds.size());
	        } else {
	            signalIds.add(requestPojo.getSignalId());
	        }

	        List<Row> signalList = accountCassandraDao.getSignalList(signalIds);
	        log.debug("Signals fetched from cassandra: {}", signalList.size());

	        if (!signalList.isEmpty()) {
	            responseData = processSignalData(signalList, bean.getAccount(), (null==bean.getServiceId()) ? 0 : bean.getServiceId(),
	                    requestPojo.getSignalId(), requestPojo.getStatus(), bean.getUserId(), bean.getRequestPayloadObject().getActualFromTime());
	            if (log.isDebugEnabled()) responseData.forEach(signal -> log.debug("Signal: {}.", signal));
	        }

	        return responseData;
	}
	
	private String getServiceIdentifier(int accountId, int serviceId) {
		try {
			Controller controller = controllerDao.getControllerListByAccountIdAndApplicationId(accountId, serviceId);
			if (controller != null) {
				return controller.getIdentifier();
			}

		} catch (Exception e) {
			log.error("Error occurred while validating service id.", e);
		}
		return "";
	}

	private Long checkSignalWindowTime(Long fromTime, Long toTime) {
		if ((null != fromTime && null != toTime)) {
			long signalWindowTime = toTime - TimeUnit.MINUTES.toMillis(SIGNAL_CLOSE_WINDOW_TIME);
			if (fromTime > signalWindowTime) {
				fromTime = signalWindowTime;
			}
		}
		return fromTime;
	}

}
