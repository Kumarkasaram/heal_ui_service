package com.heal.dashboard.service.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.heal.dashboard.service.businesslogic.*;
import com.heal.dashboard.service.pojo.ApplicationDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.ApplicationSDMRequestBean;
import com.heal.dashboard.service.beans.DateComponentBean;
import com.heal.dashboard.service.beans.MasterFeaturesBean;
import com.heal.dashboard.service.beans.SignalData;
import com.heal.dashboard.service.beans.SignalRequestPojo;
import com.heal.dashboard.service.beans.TFPRequestData;
import com.heal.dashboard.service.beans.TagMapping;
import com.heal.dashboard.service.beans.TopologyDetails;
import com.heal.dashboard.service.beans.TopologyValidationResponseBean;
import com.heal.dashboard.service.beans.UserAccessAccountsBean;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.tpf.TFPServiceDetails;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.pojo.ResponseBean;
import com.heal.dashboard.service.util.JsonFileParser;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Api(value = "Accounts")
public class AccountController {

    @Autowired
    JsonFileParser headersParser;
    @Autowired
    GetAccountsBL getAccountsBL;
    @Autowired
    UserPreferencesBL userPreferencesBL;;
    @Autowired
    TopologyServiceBL topologyServiceBL;
    @Autowired
    DateComponentBL dateComponentBL;
    @Autowired
    MasterFeaturesBL masterFeaturesBL;
    @Autowired
    TransactionFlowPathInboundBL transactionFlowPathInboundBL;
    
    @Autowired
    TransactionFlowPathOutboundBL transactionFlowPathOutboundBL;
    @Autowired
    ClusterKpiMappingServiceBL clusterKpiMappingServiceBL;
    @Autowired
    ApplicationSDMCategoryEventServiceBL applicationSDMCategoryEventServiceBL;
    @Autowired
    SignalDataServiceBL signalDataServiceBL;

    @Autowired
    ServiceApplicationBL serviceApplicationBL;

    @ApiOperation(value = "Retrieve accounts list", response = AccountBean.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts", method = RequestMethod.GET)
    public ResponseEntity<ResponseBean<List<AccountBean>>> getAccountList(@RequestHeader(value = "Authorization", required = false) String authorizationToken)
            throws ClientException, ServerException, DataProcessingException {

        UtilityBean<String> utilityBean = getAccountsBL.clientValidation(null, authorizationToken);
        UserAccessAccountsBean userAccessBean = getAccountsBL.serverValidation(utilityBean);
        List<AccountBean> accounts = getAccountsBL.process(userAccessBean);
        ResponseBean<List<AccountBean>> responseBean = new ResponseBean<>("Accounts fetching successfully", accounts, HttpStatus.OK);

        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(responseBean);
    }

    @ApiOperation(value = "Retrieve account-wise topology details", response = TopologyDetails.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}/topology", method = RequestMethod.GET)
    public ResponseEntity<ResponseBean<TopologyDetails>> getTopologyDetails(@RequestHeader(value = "Authorization") String
                                                                      authorizationToken, @PathVariable("identifier") String
                                                                      identifier, @RequestParam(value = "applicationId", required = false) String applicationId)
            throws ClientException, ServerException, DataProcessingException {

        UtilityBean<String> utilityBean = topologyServiceBL.clientValidation(null, authorizationToken, identifier, applicationId);
        TopologyValidationResponseBean topologyValidationResponseBean = topologyServiceBL.serverValidation(utilityBean);
        TopologyDetails topologyDetails = topologyServiceBL.process(topologyValidationResponseBean);
        ResponseBean<TopologyDetails> responseBean = new ResponseBean<>("Topology fetching successfully", topologyDetails, HttpStatus.OK);

        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(responseBean);
    }

    @ApiOperation(value = "Retrieve features List", response = MasterFeaturesBean.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/features", method = RequestMethod.GET)
    public ResponseEntity<List<MasterFeaturesBean>> getMasterFeatures(@RequestHeader(value = "Authorization") String authorizationToken)
            throws ClientException, DataProcessingException {
        masterFeaturesBL.clientValidation(null, authorizationToken);
        List<MasterFeaturesBean> response = masterFeaturesBL.process(null);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(response);
    }

    @ApiOperation(value = "Retrieve date components List", response = DateComponentBean.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/date-components", method = RequestMethod.GET)
    public ResponseEntity<List<DateComponentBean>> getDateTimeDropdownList(@RequestHeader(value = "Authorization") String authorizationToken)
            throws ClientException, DataProcessingException {
        dateComponentBL.clientValidation(null, authorizationToken);
        List<DateComponentBean> response = dateComponentBL.process(null);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(response);
    }
    
    @ApiOperation(value = "Retrieve user-preferences list", response = AccountBean.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/account/{identifier}/user-preferences", method = RequestMethod.GET)
    public ResponseEntity<List<TagMapping>> getPreferenceList(@RequestHeader(value = "Authorization") String authorizationToken,@PathVariable("identifier") String identifier)
            throws ClientException, DataProcessingException, ServerException {
    	UtilityBean<String> utilityBean = userPreferencesBL.clientValidation(null, authorizationToken,identifier);
    	TagMapping tagMapping = userPreferencesBL.serverValidation(utilityBean);
        List<TagMapping> response = userPreferencesBL.process(tagMapping);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(response);
    }
    
    @ApiOperation(value = "Retrieve Inbounds list", response = TFPServiceDetails.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}/applications/{applicationId}/inbounds", method = RequestMethod.GET)
    public ResponseEntity<List<TFPServiceDetails>> getInbounds(@RequestHeader(value = "Authorization") String authorizationToken,@PathVariable("identifier") String identifier,
    		@PathVariable("applicationId") String applicationId,
    		@RequestParam(value ="toTime",required =true) String toTime,
    		@RequestParam(value = "fromTime",required =true) String fromTime)
            throws ClientException, DataProcessingException, ServerException {
    	  UtilityBean<Map> utilityBean = transactionFlowPathInboundBL.clientValidation(null, authorizationToken, identifier, applicationId,fromTime,toTime);
          TFPRequestData tFPRequestData = transactionFlowPathInboundBL.serverValidation(utilityBean);
          List<TFPServiceDetails> tFPServiceDetailList = transactionFlowPathInboundBL.process(tFPRequestData);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(tFPServiceDetailList);
    }
    
    @ApiOperation(value = "Retrieve outbounds list", response = TFPServiceDetails.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}/applications/{applicationId}/outbounds", method = RequestMethod.GET)
    public ResponseEntity<List<TFPServiceDetails>> getoutBounds(@RequestHeader(value = "Authorization") String authorizationToken,@PathVariable("identifier") String identifier,
    		@PathVariable("applicationId") String applicationId,
    		@RequestParam(value ="toTime",required =true) String toTime,
    		@RequestParam(value = "fromTime",required =true) String fromTime)
            throws ClientException, DataProcessingException, ServerException {
    	  UtilityBean<Map> utilityBean = transactionFlowPathOutboundBL.clientValidation(null, authorizationToken, identifier, applicationId,fromTime,toTime);
          TFPRequestData tFPRequestData = transactionFlowPathOutboundBL.serverValidation(utilityBean);
          List<TFPServiceDetails> tFPServiceDetailList = transactionFlowPathOutboundBL.process(tFPRequestData);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(tFPServiceDetailList);
    }

    @ApiOperation(value = "Retrieve application list", response = ApplicationDetails.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}/services/{serviceId}/applications", method = RequestMethod.GET)
    public ResponseEntity<Set<ApplicationDetails>> getServiceApplications(@RequestHeader(value = "Authorization") String authorizationToken,@PathVariable("identifier") String identifier,
                                                                @PathVariable("serviceId") String serviceId)
            throws ClientException, DataProcessingException, ServerException {
        UtilityBean<String> utilityBean = serviceApplicationBL.clientValidation(null, authorizationToken, identifier, serviceId);
        utilityBean = serviceApplicationBL.serverValidation(utilityBean);
        Set<ApplicationDetails> applicationList = serviceApplicationBL.process(utilityBean);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(applicationList);
    }
  
    @ApiOperation(value = "Retrieve Category event list", response = CategoryEvents.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}/services/{serviceId}/categories/anomalies", method = RequestMethod.GET)
    public ResponseEntity<List<CategoryEvents>> getCategoryEventList(@RequestHeader(value = "Authorization") String authorizationToken,@PathVariable("identifier") String identifier,
    		@PathVariable("serviceId") String serviceId,
    		@RequestParam("load") String load,
    		@RequestParam(value ="toTime",required =true) String toTime,
    		@RequestParam(value = "fromTime",required =true) String fromTime)
            throws ClientException, DataProcessingException, ServerException {
    	  UtilityBean<Map> utilityBean = applicationSDMCategoryEventServiceBL.clientValidation(null, authorizationToken, identifier, load, serviceId,fromTime,toTime);
    	  ApplicationSDMRequestBean accADMreqBean = applicationSDMCategoryEventServiceBL.serverValidation(utilityBean);
          List<CategoryEvents> categoryEventsList = applicationSDMCategoryEventServiceBL.process(accADMreqBean);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(categoryEventsList);
    }
  
     @ApiOperation(value = "Retrieve attribule detail", response = HashMap.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}/instances/{instanceId}/attributes", method = RequestMethod.GET)
    public ResponseEntity<Map<String,String>> getAttribute(
            @RequestHeader(value = "Authorization", required = false) String authorizationToken,
            @PathVariable("identifier") String identifier,
            @PathVariable("instanceId") String instanceId)
            throws ClientException, ServerException, DataProcessingException {
        UtilityBean<Map<String, Object>> utilityBean = clusterKpiMappingServiceBL.clientValidation(null, authorizationToken, identifier,instanceId);
        UtilityBean<Map<String, Object>>  serverValidation = clusterKpiMappingServiceBL.serverValidation(utilityBean);
        Map<String,String> result  = clusterKpiMappingServiceBL.process(serverValidation);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(result);
 
        
    }
     
     @ApiOperation(value = "Retrieve  SignalData list", response = SignalData.class)
     @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
             @ApiResponse(code = 500, message = "Internal Server Error"),
             @ApiResponse(code = 400, message = "Invalid Request")})
     @RequestMapping(value = "/accounts/{identifier}/signals", method = RequestMethod.GET)
     public ResponseEntity<Set<SignalData>> getSignalList(@RequestHeader(value = "Authorization") String authorizationToken,@PathVariable("identifier") String identifier
     		,@RequestParam(value = "status",required = false) String status,@RequestParam(value = "fromTime",required = false) String fromTime,
     		@RequestParam(value = "toTime",required = false) String toTime)
             throws ClientException, DataProcessingException, ServerException {
     	
     	Map<String,String> queryParam = new HashMap<String,String>();
     	queryParam.put("signalId", null);
		queryParam.put("serviceId", null);
     	queryParam.put("status", status);
     	queryParam.put("toTime", toTime);
     	queryParam.put("fromTime", fromTime);

         UtilityBean<SignalRequestPojo> utilityBean = signalDataServiceBL.clientValidation(queryParam, authorizationToken, identifier);
         utilityBean = signalDataServiceBL.serverValidation(utilityBean);
         Set<SignalData> signalList = signalDataServiceBL.process(utilityBean);
         return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(signalList);
     }
     
		@ApiOperation(value = "Retrieve  signal list", response = SignalData.class)
		@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved data"),
				@ApiResponse(code = 500, message = "Internal Server Error"),
				@ApiResponse(code = 400, message = "Invalid Request") })
		@RequestMapping(value = "/accounts/{identifier}/services/{serviceId}/signals", method = RequestMethod.GET)
		public ResponseEntity<Set<SignalData>> getSignalListBySeviceId(
				@RequestHeader(value = "Authorization") String authorizationToken,
				@PathVariable("identifier") String identifier, @PathVariable("serviceId") String serviceId,
				@RequestParam(value = "status", required = false) String status,
				@RequestParam(value = "fromTime", required = false) String fromTime,
				@RequestParam(value = "toTime", required = false) String toTime)
				throws ClientException, DataProcessingException, ServerException {

			Map<String,String> queryParam = new HashMap<>();
			queryParam.put("signalId", null);
			queryParam.put("serviceId", serviceId);
			queryParam.put("status", status);
			queryParam.put("toTime", toTime);
			queryParam.put("fromTime", fromTime);

			UtilityBean<SignalRequestPojo> utilityBean = signalDataServiceBL.clientValidation(queryParam,
					authorizationToken, identifier);
			utilityBean = signalDataServiceBL.serverValidation(utilityBean);
			Set<SignalData> signalList = signalDataServiceBL.process(utilityBean);
			return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(signalList);
		}

		@ApiOperation(value = "Retrieve  signal list", response = SignalData.class)
		@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved data"),
				@ApiResponse(code = 500, message = "Internal Server Error"),
				@ApiResponse(code = 400, message = "Invalid Request") })
		@RequestMapping(value = "/accounts/{identifier}/signals/{signalId}", method = RequestMethod.GET)
		public ResponseEntity<Set<SignalData>> getSignalListBySignalId(
				@RequestHeader(value = "Authorization") String authorizationToken,
				@PathVariable("identifier") String identifier, @PathVariable("signalId") String signalId,
				@RequestParam(value = "status", required = false) String status,
				@RequestParam(value = "fromTime", required = false) String fromTime,
				@RequestParam(value = "toTime", required = false) String toTime)
				throws ClientException, DataProcessingException, ServerException {

			Map<String,String> queryParam = new HashMap<String,String>();
			queryParam.put("signalId", signalId);
			queryParam.put("serviceId", null);
			queryParam.put("status", status);
			queryParam.put("toTime", toTime);
			queryParam.put("fromTime", fromTime);

			UtilityBean<SignalRequestPojo> utilityBean = signalDataServiceBL.clientValidation(queryParam,
					authorizationToken, identifier);
			utilityBean = signalDataServiceBL.serverValidation(utilityBean);
			Set<SignalData> signalList = signalDataServiceBL.process(utilityBean);
			return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(signalList);
		}
     
    
}

