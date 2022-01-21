package com.heal.dashboard.service.businesslogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datastax.driver.core.Row;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.CategoryDetailBean;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.MasterKPIDetailsBean;
import com.heal.dashboard.service.beans.MstKpi;
import com.heal.dashboard.service.beans.SignalData;
import com.heal.dashboard.service.beans.SignalRequestPojo;
import com.heal.dashboard.service.beans.SignalStatus;
import com.heal.dashboard.service.beans.SignalType;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.ViewTypeBean;
import com.heal.dashboard.service.dao.mysql.AccountCassandraDao;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CommonServiceBLUtil;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.DateTimeUtil;
import com.heal.dashboard.service.util.HealUICache;
import com.heal.dashboard.service.util.UserValidationUtil;

import antlr.StringUtils;
import io.undertow.security.idm.Account;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SignalDataServiceBL implements BusinessLogic<SignalRequestPojo, UtilityBean<SignalRequestPojo>, Set<SignalData>> {

	private static final long SIGNAL_CLOSE_WINDOW_TIME = 0;
	@Autowired
	AccountDao accountDao;
	@Autowired
	ControllerDao controllerDao;
	@Autowired
	AccountCassandraDao accountCassandraDao;
	@Autowired
	UserValidationUtil userValidationUtil;
	@Autowired
	MasterDataDao masterDataDao;
	@Autowired
	CommonServiceBLUtil commonServiceUtil;

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

		return UtilityBean.<SignalRequestPojo>builder().accountIdentifier(identifier).serviceId(serviceIdString.valueOf(serviceId))
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

	        utilityBean.setAccountBean(account);
	        return utilityBean;
	}

	@Override
	public Set<SignalData> process(UtilityBean<SignalRequestPojo> bean) throws DataProcessingException {
		  SignalRequestPojo requestPojo = bean.getPojoObject();

	        Set<SignalData> responseData = new HashSet<>();

	        Set<String> signalIds = new HashSet<>();
	        if (requestPojo.getSignalId() == null) {
	            try {
					signalIds.addAll(accountCassandraDao.getSignalId(bean.getAccountIdentifier(), requestPojo.getFromTime(), requestPojo.getToTime()));
				} catch (ServerException e) {
	                 throw new DataProcessingException(e.getMessage());

				}
	            log.debug("Signal Ids fetched from cassandra: {}", signalIds.size());
	        } else {
	            signalIds.add(requestPojo.getSignalId());
	        }

	        List<Row> signalList;
			try {
				signalList = accountCassandraDao.getSignalList(signalIds);
			} catch (ServerException e) {
				 throw new DataProcessingException(e.getMessage());
			}
	        log.debug("Signals fetched from cassandra: {}", signalList.size());

	        if (!signalList.isEmpty()) {
	            responseData = processSignalData(signalList, bean.getAccountBean(), (null==bean.getServiceId()) ? 0 : Integer.parseInt(bean.getServiceId()),
	                    requestPojo.getSignalId(), requestPojo.getStatus(), bean.getAuthToken(), bean.getPojoObject().getActualFromTime());
	            if (log.isDebugEnabled()) responseData.forEach(signal -> log.debug("Signal: {}.", signal));
	        }
	        return responseData;
	       
	}
	protected Set<SignalData> processSignalData(List<Row> resultSetList, AccountBean account, Integer serviceId,
			String signalId, String status, String userId, Long actualFromTime) throws DataProcessingException {
		Set<SignalData> responseData;

		 UserAccessDetails userAccessDetails = userValidationUtil.getUserAccessDetails(userId, account.getIdentifier());
	        if(userAccessDetails == null) {
	            log.error("User access details unavailable for user [{}]", userId);
	            throw new DataProcessingException("User access details unavailable");
	        }

		Map<String, Controller> controllerMap;
		try {
			controllerMap = controllerDao.getControllerList(account.getAccountId()).stream()
					.collect(Collectors.toMap(Controller::getIdentifier, java.util.function.Function.identity()));
		} catch (ServerException e) {
			  throw new DataProcessingException(e.getMessage());
		}
		responseData = resultSetList.stream()
				.filter(signal -> null == status || signal.getString("current_status").equals(status))
				.map(signal -> {
					try {
						return getSignalData(signal, serviceId, account, signalId,
								userAccessDetails.getApplicationIdentifiers(), actualFromTime, controllerMap);
					} catch (DataProcessingException e) {
						// TODO Auto-generated catch block
			            return null;

					}
				})
				.filter(Objects::nonNull).collect(Collectors.toSet());

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
	
	private SignalData getSignalData(Row problemRow, Integer serviceId, AccountBean account, String signalId,
			List<String> assignedApplicationsToCurrentUser, Long actualFromTime,
			Map<String, Controller> controllerMap) throws DataProcessingException {
		Set<String> serviceList = problemRow.getSet("service_ids", String.class);

		if (serviceId == 0 || (serviceList != null
				&& serviceList.contains(getServiceIdentifier(account.getAccountId(), serviceId)))) {

			if (problemRow.getString("signal_type").equalsIgnoreCase(SignalType.INFO.name())
					&& problemRow.getLong("start_time") < actualFromTime) {
				log.warn("Signal Skipped Type: {}, startTime: {}, actualFromTime: {}", SignalType.INFO.name(),
						problemRow.getLong("start_time"), actualFromTime);
				return null;
			}

			SignalData signalData = populateSignalData(problemRow, account, controllerMap);
			if (null == signalData) {
				return null;
			}

			for (String svc : serviceList) {
				if (controllerMap.containsKey(svc)) {
					signalData.addServices(controllerMap.get(svc));
				}
			}
			boolean addableProblem = (signalId == null || signalData.getId().trim().equals(signalId.trim()));
			if (SignalType.BATCH_JOB.getDisplayName().equals(signalData.getType()) && !serviceList.isEmpty()) {
				java.util.Optional<Controller> appFound = null;
				try {
					appFound = controllerDao.getApplicationList(account.getAccountId())
							.stream().filter(app -> serviceList.contains(app.getIdentifier())).findAny();
				} catch (ServerException e) {
					// TODO Auto-generated catch block
		            throw new DataProcessingException(e.getMessage());
				}
				if (appFound.isPresent() && addableProblem) {
					List<Controller> appList = new ArrayList<>();
					appList.add(appFound.get());
					signalData.addApplications(appList);
					return signalData;
				}
			} else {
				List<Controller> affectedApplications = getAffectedApplications(account, serviceList);

				long assignedAffectedApplications = affectedApplications.stream()
						.filter(affApp -> assignedApplicationsToCurrentUser.contains(affApp.getIdentifier())).count();

//Check if the problem should be added.

				if (addableProblem && assignedAffectedApplications != 0) {
					signalData.addApplications(affectedApplications);
					return signalData;
				}
			}
		}
		return null;
	}

	protected SignalData populateSignalData(Row signal, AccountBean account, Map<String, Controller> controllerMap) {
		SignalData signalData = null;
		if (signal != null) {
			signalData = new SignalData();
			Map<String, String> metaData = signal.getMap("meta_data", String.class, String.class);
			if (metaData.containsKey("kpi_category_id")) {
				CategoryDetailBean categoryDetailBean = masterDataDao
						.getCategoryDetailsForCategory(metaData.get("kpi_category_id"));
				if (null == categoryDetailBean) {
					log.error("categoryDetailBean is null for signalId: {}, kpi_category_id: {}",
							signal.getString("signal_id"), metaData.get("kpi_category_id"));
					signalData.setMetricCategory(null);
				} else {
					signalData.setMetricCategory(categoryDetailBean.getName());
				}
			}

			ViewTypeBean typeDetails = masterDataDao
					.getMstSubTypeForSubTypeId(Integer.parseInt(signal.getString("severity")));
			String time = metaData.get("end_time");
			signalData.setId(signal.getString("signal_id"));
			signalData.setType(getDisplayName(signal));
//IO-2103 - if the signal processor has not updated the signal in the signal window time then we will mark it as closed.
			if (shouldProblemBeClosed(signal) && !SignalType.BATCH_JOB.getDisplayName().equals(signalData.getType())) {
				signalData.setCurrentStatus(SignalStatus.CLOSED.getReturnType());
			} else {
				signalData.setCurrentStatus(
						SignalStatus.valueOf(signal.getString(Constants.LITERAL_CURRENT_STATUS)).getReturnType());
			}
			signalData.setSeverity(typeDetails.getSubTypeName());
			signalData.setStartTimeMilli(signal.getLong("start_time"));
			signalData.setUpdatedTimeMilli((time == null) ? 0
					: Long.valueOf(signal.getMap("meta_data", String.class, String.class).get("end_time")));
			signalData.setDescription(getProblemDescription(signal, account, controllerMap));
			Set<String> anomalyIdSet = signal.getSet("anomalies", String.class);
			long anomalyCount = (anomalyIdSet == null) ? 0 : anomalyIdSet.size();
			log.debug("Number of anomaly events in signal: {}, is {}.", signalData.getId(), anomalyCount);
			signalData.setEventCount(anomalyCount);
			Long txnAnomalyTime = signal.get("txn_anomaly_time", Long.class);
			Long kpiAnomalyTime = signal.get("updated_time", Long.class);
			if (txnAnomalyTime != null || kpiAnomalyTime != null) {
				if (txnAnomalyTime == null) {
					signalData.setLastEventTime(kpiAnomalyTime);
				} else if (kpiAnomalyTime == null) {
					signalData.setLastEventTime(txnAnomalyTime);
				} else {
					signalData.setLastEventTime((txnAnomalyTime >= kpiAnomalyTime) ? txnAnomalyTime : kpiAnomalyTime);
				}
			}
		}
		return signalData;
	}

	  private boolean shouldProblemBeClosed(Row signal){
	        log.info("SIGNAL_CLOSE_WINDOW_TIME: {}, id: {}", SIGNAL_CLOSE_WINDOW_TIME,  signal.getString("signal_id"));
	        if((SignalStatus.valueOf(signal.getString(Constants.LITERAL_CURRENT_STATUS)).getReturnType().equalsIgnoreCase(SignalStatus.OPEN.getReturnType()))){
	            long updatedTime = (null == signal.get("updated_time", Long.class)) ? 0 : signal.get("updated_time", Long.class);
	            long txnAnomalyTime = (null == signal.get("txn_anomaly_time", Long.class)) ? 0 : signal.get("txn_anomaly_time", Long.class);
	            long lastUpdatedTime = Math.max(updatedTime, txnAnomalyTime);
	            long signalWindowTime = DateTimeUtil.getCurrentTimestampInGMT().getTime() - TimeUnit.MINUTES.toMillis(SIGNAL_CLOSE_WINDOW_TIME);
	            log.info("shouldProblemBeClosed() signalId: {}, updated_time: {}, txn_anomaly_time: {}, windowTime: {}",
	                    signal.getString("signal_id"), updatedTime, txnAnomalyTime, SIGNAL_CLOSE_WINDOW_TIME);
	            if(lastUpdatedTime<signalWindowTime){
	            	log.info("Signal marked as closed in the signal list.");
	                return true;
	            }
	        }
	        return false;
	    }
	  
	  
	   public  String getProblemDescription(Row signal, AccountBean account, Map<String, Controller> controllerMap){
	        String entryServiceId = signal.getString("entry_service_id");
	        /*if(entryServiceId!=null){
	            desc.append("Problem: Transactions at ").append(entryServiceId).append(" have been affected.");
	        }
	        else {
	            Set<String> serviceIds = signal.getSet("root_cause_service_ids", String.class);
	            List<Controller> controllers = MasterDataService.getControllerByIdentifier(serviceIds);
	            if(controllers == null || controllers.isEmpty()) {
	                desc.append(PROBLEM_DESC);
	            } else {
	                //Event(s) in {RCA_SERVICE_NAMES} root cause service(s) may impact transaction performance
	                desc.append("Event(s) in ")
	                        .append(controllers.parallelStream().map(Controller::getName).collect(Collectors.joining()))
	                        .append(" root cause service(s) may impact transaction performance.");
	            }
	        }
	        return desc.toString();*/
	        String raw = "";
	        String signalType = signal.getString("signal_type");
	        if( signalType != null ) {
	            SignalType st = SignalType.valueOf(signalType);
	            log.debug("Signal type: {}", st.name());
	            if( SignalType.BATCH_JOB.equals(st )) {
	                raw = Constants.SIGNAL_BATCH_PROCESS_DESCRIPTION_DEFAULT;
	            }
	            else if( st.equals(SignalType.INFO) ) {
	                raw = Constants.SIGNAL_INFO_DESCRIPTION_DEFAULT;
	            } else if( entryServiceId != null ) {
	                raw = Constants.SIGNAL_PROBLEM_DESCRIPTION_DEFAULT;
	            } else {
	                raw = Constants.SIGNAL_WARNING_DESCRIPTION;
	            }
	        }

	        Map<String,String> placeHolders = extractPlaceholderData(signal,account, controllerMap);
	        for(Map.Entry<String,String> entry: placeHolders.entrySet()) {
	            raw = raw.replaceAll(entry.getKey(), entry.getValue());
	        }
	        return raw;
	    }

	    public  Map<String,String> extractPlaceholderData(Row signal, AccountBean account, Map<String, Controller> controllerMap) {
	        String KPI = "<kpi>";
	        String JOB_ID = "<job_id>";
	        String CUR_BATCH_STATUS = "<current_batch_status>";
	        String CUR_STATUS = "<current_status>";
	        String ENTRY_SVC_NAME = "<entry_service_name>";
	        String META_DATA = "<meta_data>";
	        String RELATED_SIGNALS = "<related_signals>";
	        String RC_SVC_LIST = "<root_cause_service_list>";
	        String AFFECTED_SVC_LIST = "<affected_service_list>";
	        String SEVERITY = "<severity>";
	        String SIGNAL_TYPE = "<signal_type>";
	        String NA = "NA";
	        Map<String,String> result = new HashMap<>();
	        Map<String,String> metaData = signal.getMap("meta_data", String.class, String.class);
	        String jobId = metaData.get("batch_job_id");
	        String currentBatchStatus = metaData.get("batch_job_status");
	        String kpiIdString = metaData.get("kpi_id");
	        String status = signal.getString("current_status");
	        String entrySvcId = signal.getString("entry_service_id");
	        Set<String> relatedSignals = signal.getSet("related_signals", String.class);
	        Set<String> rootCauseServiceIdSet = signal.getSet("root_cause_service_ids", String.class);
	        Set<String> affectedSvcList = signal.getSet("service_ids", String.class);
	        String severityId = signal.getString("severity");
	        String signalType = signal.getString("signal_type");
	        MasterKPIDetailsBean masterKPIDetailsBean = null;
	        if (!(kpiIdString.isEmpty()) && account != null) {
	            int kpi_id = Integer.parseInt(kpiIdString);
	            MstKpi mstKpi = new MstKpi();
	            mstKpi.setKpiId(kpi_id);
	            mstKpi.setAccountId(account.getAccountId());
	            masterKPIDetailsBean = masterDataDao.getMasterKPIDetailsData(mstKpi.getKpiId(),mstKpi.getDefaultAccountId(),mstKpi.getAccountId());

	        }
	        result.put(KPI, masterKPIDetailsBean != null ? masterKPIDetailsBean.getName():NA);
	        String rootCauseServiceNames = rootCauseServiceIdSet.stream()
	                .map(identifier -> controllerMap.getOrDefault(identifier, null))
	                .filter(Objects::nonNull)
	                .map(Controller::getName)
	                .collect(Collectors.joining());
	        String affectedServiceNames = affectedSvcList.stream()
	                .map(identifier -> controllerMap.getOrDefault(identifier, null))
	                .filter(Objects::nonNull)
	                .map(Controller::getName)
	                .collect(Collectors.joining());
	        String entryServiceName = ( entrySvcId == null ) ? NA : controllerMap.containsKey(entrySvcId) ? controllerMap.get(entrySvcId).getName(): NA;
	        String severity = NA;
	        if( severityId != null ) {
	            int sevId = Integer.parseInt(severityId);
	            ViewTypeBean viewTypes = masterDataDao.getMstSubTypeForSubTypeId(sevId);
	            if(viewTypes != null)  severity = viewTypes.getSubTypeName();
	        }
	        result.put(CUR_BATCH_STATUS,currentBatchStatus!=null?currentBatchStatus:NA);
	        result.put(JOB_ID,jobId!=null?jobId:NA);
	        result.put(CUR_STATUS, status);
	        result.put(ENTRY_SVC_NAME, entryServiceName);
	        result.put(META_DATA, metaData.toString());
	        result.put(RELATED_SIGNALS, (relatedSignals == null) ? NA : String.join(",", relatedSignals));
	        result.put(RC_SVC_LIST, rootCauseServiceNames);
	        result.put(AFFECTED_SVC_LIST, affectedServiceNames);
	        result.put(SEVERITY, severity);
	        result.put(SIGNAL_TYPE, (signalType == null) ? NA : signalType);

	        return result;
	    }
	    
	    protected List<Controller> getAffectedApplications(AccountBean account, Set<String> affectedServices) {
	        if( affectedServices == null || affectedServices.isEmpty() ) {
	            log.error("Invalid affected service list found in cassandra.");
	            return new ArrayList<>();
	        }
	        Map<String, List<Controller>> serviceApplicationMap = commonServiceUtil.getServiceApplicationMap(account.getAccountId());

	        Set<Controller> appList = new HashSet<>();
	        affectedServices.forEach(svc -> {
	            if (serviceApplicationMap.get(svc) != null) {
	                appList.addAll(serviceApplicationMap.get(svc));
	            }
	        });
	        return new ArrayList<>(appList);
	    }
	    
	    private String getDisplayName(Row signal){
	        String dispName = null;
	        try {
	             dispName = SignalType.valueOf(signal.getString("signal_type")).getDisplayName();
	        }
	        catch (Exception e){
	            log.error("Error occurred in enum SignalType: values{}", signal.getString("signal_type"), e);
	        }
	        return dispName;
	    }
}