package com.heal.dashboard.service.controller;

import com.heal.dashboard.service.beans.*;
import com.heal.dashboard.service.businesslogic.ApplicationHealthBL;
import com.heal.dashboard.service.businesslogic.ApplicationSDMServiceBL;
import com.heal.dashboard.service.businesslogic.FileUploadServiceBL;
import com.heal.dashboard.service.businesslogic.GetApplicationBL;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.pojo.ApplicationDetails;
import com.heal.dashboard.service.util.JsonFileParser;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class ApplicationsController {

    @Autowired
    JsonFileParser headersParser;
    @Autowired
    GetApplicationBL getApplicationBL;
    @Autowired
    ApplicationHealthBL applicationHealthBL;
    @Autowired
    ApplicationSDMServiceBL applicationSDMServiceBL;
    @Autowired
    FileUploadServiceBL fileUploadServiceBL;

    @ApiOperation(value = "Retrieve applications List", response = ApplicationDetails.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}/applications", method = RequestMethod.GET)
    public ResponseEntity<List<ApplicationDetails>> getApplicationList(@RequestHeader(value = "Authorization") String authorizationToken, @PathVariable("identifier") String identifier)
            throws ClientException, ServerException, DataProcessingException {
        UtilityBean<String> utilityBean = getApplicationBL.clientValidation(null, authorizationToken, identifier);
        UserAccessDetails userAccessDetails = getApplicationBL.serverValidation(utilityBean);
        List<ApplicationDetails> applicationDetails = getApplicationBL.process(userAccessDetails);

        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(applicationDetails);
    }

    @ApiOperation(value = "Retrieve Application Health", response = AccountBean.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}/application-health", method = RequestMethod.GET)
    public ResponseEntity<List<ApplicationHealthDetail>> getApplicationHealthStatus(@RequestHeader(value = "Authorization") String authorizationToken, @PathVariable(value = "identifier") String identifier,
                                                                                    @RequestParam(value = "toTime") String toTimeString) throws ClientException, ServerException, DataProcessingException {
        UtilityBean<String> utilityBean = applicationHealthBL.clientValidation(null, identifier, toTimeString, authorizationToken);
        UserAccessDetails userAccessDetails = applicationHealthBL.serverValidation(utilityBean);
        List<ApplicationHealthDetail> response = applicationHealthBL.process(userAccessDetails);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(response);
    }

//    @ApiOperation(value = "Retrieve application Details", response = TopologyDetails.class)
//    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
//            @ApiResponse(code = 500, message = "Internal Server Error"),
//            @ApiResponse(code = 400, message = "Invalid Request")})
//    @RequestMapping(value = "/accounts/{identifier}", method = RequestMethod.GET)
//    public ResponseEntity<ResponseBean<TopologyDetails>> getFileUploadDetails1(
//            @RequestHeader(value = "Authorization", required = false) String authorizationToken,
//            @PathVariable("identifier") String identifier,
//            @PathVariable("applicationId") String applicationId,
//            @RequestParam(value ="toTime",required =true) String toTime,
//            @RequestParam(value = "fromTime",required =true) String fromTime)
//            throws ClientException, ServerException, DataProcessingException {
//
//        UtilityBean<Map<String,String>> utilityBean = applicationSDMServiceBL.clientValidation(null, authorizationToken, identifier, toTime, fromTime);
//        ApplicationSDMRequestBean applicationSDMRequestBean = applicationSDMServiceBL.serverValidation(utilityBean);
//        TopologyDetails topologyDetails = applicationSDMServiceBL.process(applicationSDMRequestBean);
//        ResponseBean<TopologyDetails> responseBean = new ResponseBean<>("Service-wise topology fetching successfully", topologyDetails, HttpStatus.OK);
//        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(responseBean);
//    }

    @ApiOperation(value = "Retrieve file upload detail", response = HashMap.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully retrieved data"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 400, message = "Invalid Request")})
    @RequestMapping(value = "/accounts/{identifier}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, List<String>>> getFileUploadDetails(
            @RequestHeader(value = "Authorization", required = false) String authorizationToken,
            @PathVariable("identifier") String identifier)
            throws ClientException, ServerException, DataProcessingException {

        UtilityBean<String> utilityBean = fileUploadServiceBL.clientValidation(null, authorizationToken, identifier);
        List<FileSummaryDetailsBean> fileSummaryDetailsBeanList = fileUploadServiceBL.serverValidation(utilityBean);
        Map<String, List<String>> result  = fileUploadServiceBL.process(fileSummaryDetailsBeanList);
        return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(result);
    }

}
