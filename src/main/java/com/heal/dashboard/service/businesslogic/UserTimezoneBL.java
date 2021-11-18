package com.heal.dashboard.service.businesslogic;

import com.heal.dashboard.service.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heal.dashboard.service.beans.TagDetails;
import com.heal.dashboard.service.beans.TagMapping;
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
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.DateUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserTimezoneBL implements BusinessLogic<UserTimezoneRequestData, UserTimezoneRequestData, String> {

    @Autowired
    TimezoneDao timezoneDao;
    ObjectMapper mapper = new ObjectMapper();

    @Override
    public UtilityBean<UserTimezoneRequestData> clientValidation(Object requestBody, String... requestParams) throws ClientException {
        mapper.setSerializationInclusion(Include.NON_NULL);
    	String jwtToken = requestParams[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }

        String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        if (null == userId || userId.trim().isEmpty()) {
            throw new ClientException("User details extraction failure");
        }

        UserTimezoneRequestData userTimezoneRequestData = new UserTimezoneRequestData();
        userTimezoneRequestData.setUserTimezonePojo(mapper.convertValue(requestBody, UserTimezonePojo.class));
        userTimezoneRequestData.setUsername(requestParams[1]);

        return UtilityBean.<UserTimezoneRequestData>builder()
                .authToken(userId)
                .pojoObject(userTimezoneRequestData).build();
    }

    @Override
    public UserTimezoneRequestData serverValidation(UtilityBean<UserTimezoneRequestData> utilityBean) throws ServerException {
        UserTimezoneRequestData userTimezoneRequestData = utilityBean.getPojoObject();
        String userName = userTimezoneRequestData.getUsername();

        UserAttributeBeen userAttributeBeen = timezoneDao.getUserAttributes(userName);
        if (userAttributeBeen == null || userAttributeBeen.getStatus() != 1) {
            log.error("User [{}] is in-active for My Profile changes", utilityBean.getPojoObject().getUsername());
            throw new ServerException(String.format("User [%s] is in-active for My Profile changes", utilityBean.getPojoObject().getUsername()));
        }

        userTimezoneRequestData.setUserTimezonePojo(utilityBean.getPojoObject().getUserTimezonePojo());
        userTimezoneRequestData.setUserAttributeBeen(userAttributeBeen);

        int timeZoneId = userTimezoneRequestData.getUserTimezonePojo().getTimezoneId();
        if (timeZoneId > 0) {
            TimezoneDetail timezoneDetail = timezoneDao.getTimezonesById(String.valueOf(timeZoneId));
            userTimezoneRequestData.setTimezoneDetail(timezoneDetail);
        }

        UserDetailsBean userDetailsBean = timezoneDao.getUsers(utilityBean.getAuthToken());
        if (userDetailsBean == null) {
            log.error("User details unavailable for userId [{}]", utilityBean.getAuthToken());
            throw new ServerException("User details unavailable for userId " + utilityBean.getAuthToken());
        }

        userTimezoneRequestData.setUserDetailsBean(userDetailsBean);

        return userTimezoneRequestData;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class)
    public String process(UserTimezoneRequestData requestBean) throws DataProcessingException {

		TagDetails tagDetailsBean = timezoneDao.getTagDetails(Constants.TIME_ZONE_TAG, Constants.DEFAULT_ACCOUNT_ID);
		if(tagDetailsBean == null) {
			log.error("Error while fetching tag details for TimeZone tag");
			throw new DataProcessingException("Error while fetching tag details for TimeZone tag");
		}

		int tagMappingId = timezoneDao.getUserTagMappingId(Constants.USER_ATTRIBUTES_TABLE_NAME_MYSQL,
				requestBean.getUserAttributeBeen().getId(), tagDetailsBean.getId());

		return batchUpdate(requestBean, tagMappingId, tagDetailsBean);
	}


    public String batchUpdate(UserTimezoneRequestData requestData, int tagMappingId, TagDetails tagDetailsDean) throws DataProcessingException {

		String timeInGMT = DateUtil.getTimeInGMT(System.currentTimeMillis());
		int res = timezoneDao.updateUserTimezoneChoice(requestData.getUserTimezonePojo().getIsTimezoneMychoice(),
				requestData.getUserTimezonePojo().getIsNotificationsTimezoneMychoice(), timeInGMT,
				requestData.getUserAttributeBeen().getUsername(),
				requestData.getUserDetailsBean().getUserIdentifier());

		if (res <= 0) {
			log.error("Error while updating user timezone choice for user [{}]", requestData.getUsername());
			throw new DataProcessingException("Error while updating user timezone choice for user " + requestData.getUsername());
		}

		if (tagMappingId != 0 && requestData.getUserTimezonePojo().getTimezoneId() == 0) {
			res = timezoneDao.deleteUserTagMapping(tagMappingId); // Delete from tag_mapping

		} else if (tagMappingId != 0 && requestData.getUserTimezonePojo().getTimezoneId() != 0) {
			res = timezoneDao.updateUserTagMapping(requestData.getTimezoneDetail().getId(), timeInGMT, tagMappingId,
					requestData.getUserDetailsBean().getCreatedBy()); // Update tag_mapping

		} else if (tagMappingId == 0 && requestData.getUserTimezonePojo().getTimezoneId() != 0) {
			TagMapping tagMappingDetails = populateTagMapping(requestData, tagDetailsDean,
					requestData.getTimezoneDetail());
			res = timezoneDao.addUserTagMapping(tagMappingDetails); // Insert into tag_mapping
		}

		if (res <= 0) {
			log.error("Failed to update timezone for userId [{}]", requestData.getUserAttributeBeen().getId());
			throw new DataProcessingException("Failed to update timezone for user id " + requestData.getUserDetailsBean().getId());
		}

		return requestData.getUserDetailsBean().getUserIdentifier();
	}

    public TagMapping populateTagMapping(UserTimezoneRequestData configData, TagDetails tagDetailsBean, TimezoneDetail timezoneDetail) {
        TagMapping tagMappingDetails = new TagMapping();

        tagMappingDetails.setAccountId(Constants.DEFAULT_ACCOUNT_ID);
        tagMappingDetails.setObjectId(configData.getUserAttributeBeen().getId());
        tagMappingDetails.setObjectRefTable(Constants.USER_ATTRIBUTES_TABLE_NAME_MYSQL);
        tagMappingDetails.setTagValue(String.valueOf(timezoneDetail.getTimeOffset()));
        tagMappingDetails.setTagId(tagDetailsBean.getId());
        tagMappingDetails.setTagKey(String.valueOf(configData.getUserTimezonePojo().getTimezoneId()));
        tagMappingDetails.setUserDetailsId(configData.getUserDetailsBean().getCreatedBy());

        return tagMappingDetails;
    }
}
