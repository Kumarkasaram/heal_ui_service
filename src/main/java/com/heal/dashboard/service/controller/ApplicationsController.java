package com.heal.dashboard.service.controller;

import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.ApplicationHealthDetail;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.businesslogic.ApplicationHealthBL;
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

import java.util.List;

@Slf4j
@RestController
public class ApplicationsController {

    @Autowired
    JsonFileParser headersParser;
    @Autowired
    GetApplicationBL getApplicationBL;
    @Autowired
    ApplicationHealthBL applicationHealthBL;

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
}
