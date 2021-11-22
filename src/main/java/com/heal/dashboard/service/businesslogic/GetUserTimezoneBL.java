package com.heal.dashboard.service.businesslogic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heal.dashboard.service.beans.TagDetails;
import com.heal.dashboard.service.beans.TimezoneDetail;
import com.heal.dashboard.service.beans.UserAttributeBeen;
import com.heal.dashboard.service.beans.UserDetailsBean;
import com.heal.dashboard.service.beans.UserTimezonePojo;
import com.heal.dashboard.service.beans.UserTimezoneRequestData;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.TimezoneDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GetUserTimezoneBL implements BusinessLogic<String, UserTimezoneRequestData, UserTimezonePojo> {

    @Autowired
    TimezoneDao timezoneDao;
    ObjectMapper mapper = new ObjectMapper();

    @Override
    public UtilityBean<String> clientValidation(Object requestBody, String... requestParams) throws ClientException {
        mapper.setSerializationInclusion(Include.NON_NULL);
    	String jwtToken = requestParams[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }

        String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        if (null == userId || userId.trim().isEmpty()) {
            throw new ClientException("User details extraction failure");
        }
        String userName = requestParams[1];
        if (null == userName || userName.trim().isEmpty()) {
            throw new ClientException("username cant be null or empty");
        }
    
        return UtilityBean.<String>builder()
                .authToken(userId)
                .pojoObject(userName).build();
    }

    @Override
    public UserTimezoneRequestData serverValidation(UtilityBean<String> utilityBean) throws ServerException {
    	String userId = utilityBean.getPojoObject();
    	  UserAttributeBeen userAttributeBeen = timezoneDao.getUserAttributes(utilityBean.getPojoObject());
          if (userAttributeBeen == null || userAttributeBeen.getStatus() != 1) {
        	  log.error(Constants.MESSAGE_INVALID_PARAMETERS, utilityBean.getPojoObject());
             
              throw new ServerException("Invalid input parameters provided "+utilityBean.getPojoObject());
          }
        UserDetailsBean userDetailBeen = timezoneDao.getUsers(userId);
        if (userDetailBeen == null || userDetailBeen.getStatus() != 1) {
            log.error("User [{}] is in-active for My Profile changes", utilityBean.getPojoObject());
            throw new ServerException(String.format("User [%s] is in-active for My Profile changes", utilityBean.getPojoObject()));
        }
        UserTimezoneRequestData userTimezoneRequestData = new UserTimezoneRequestData();
        userTimezoneRequestData.setUserAttributeBeen(userAttributeBeen);
        userTimezoneRequestData.setUserDetailsBean(userDetailBeen);

        int timeZoneId = userTimezoneRequestData.getUserTimezonePojo().getTimezoneId();
        if (timeZoneId > 0) {
            TimezoneDetail timezoneDetail = timezoneDao.getTimezonesById(String.valueOf(timeZoneId));
            userTimezoneRequestData.setTimezoneDetail(timezoneDetail);
        }

        return userTimezoneRequestData;
    }

    @Override
    public UserTimezonePojo process(UserTimezoneRequestData configData) throws DataProcessingException {

		TagDetails tagDetailsBean = timezoneDao.getTagDetails(Constants.TIME_ZONE_TAG, Constants.DEFAULT_ACCOUNT_ID);
		if(tagDetailsBean == null) {
			log.error("Error while fetching tag details for TimeZone tag");
			throw new DataProcessingException("Error while fetching tag details for TimeZone tag");
		}
		 TimezoneDetail timezoneDetail = timezoneDao.getTimezoneByUser(Constants.USER_ATTRIBUTES_TABLE_NAME_MYSQL,configData.getUserDetailsBean().getId(),tagDetailsBean.getId());
		    UserTimezonePojo userTimezonePojo = new UserTimezonePojo();
	        if(null==timezoneDetail){
	            userTimezonePojo.setTimezoneId(0);
	        }
	        else {
	            userTimezonePojo.setTimezoneId(timezoneDetail.getId());
	        }
	        /*`isTimezoneMychoice` & `isNotificationsTimezoneMychoice` is saved inverted in DB as per the UI-English
	         * if checkbox is checked then `isTimezoneMychoice`=0 & `isNotificationsTimezoneMychoice`=0
	         * if checkbox is un-checked then `isTimezoneMychoice`=1 & `isNotificationsTimezoneMychoice`=1
	         * */
	        userTimezonePojo.setIsTimezoneMychoice(configData.getUserAttributeBeen().getIsTimezoneMychoice()^1);
	        userTimezonePojo.setIsNotificationsTimezoneMychoice(configData.getUserAttributeBeen().getIsNotificationsTimezoneMychoice()^1);
	        return userTimezonePojo;

	}


  
}
