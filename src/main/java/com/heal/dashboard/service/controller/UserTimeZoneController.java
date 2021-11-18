package com.heal.dashboard.service.controller;



import com.heal.dashboard.service.beans.UserTimezonePojo;
import com.heal.dashboard.service.beans.UserTimezoneRequestData;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.businesslogic.UserTimezoneBL;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.JsonFileParser;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class UserTimeZoneController {

	@Autowired
	UserTimezoneBL userTimezoneBL;
	@Autowired
	JsonFileParser headersParser;

	@ApiOperation(value = "ADD/Update user tagging Detail", response = String.class)
	 @ApiResponses(value = {@ApiResponse(code = 200, message = "Successfully created|Updated data"),
	            @ApiResponse(code = 500, message = "Internal Server Error"),
	            @ApiResponse(code = 400, message = "Invalid Request")})
	@PostMapping(value = "/users/{username}/timezones")
	public ResponseEntity<String> setUserPreferedZone(@PathVariable(name = "username") String userName,
			@RequestHeader(value = "Authorization") String authorizationToken,
			@Valid @RequestBody UserTimezonePojo tzResponse) throws ClientException, ServerException, DataProcessingException {

		UtilityBean<UserTimezoneRequestData> utilityBean = userTimezoneBL.clientValidation(tzResponse, authorizationToken, userName);
		UserTimezoneRequestData userTimezoneRequestData = userTimezoneBL.serverValidation(utilityBean);
		String response = userTimezoneBL.process(userTimezoneRequestData);

		return ResponseEntity.ok().headers(headersParser.loadHeaderConfiguration()).body(response);
	}
}

