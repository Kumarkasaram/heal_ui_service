package com.heal.dashboard.service.businesslogic;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.heal.dashboard.service.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.AccountMappingBean;
import com.heal.dashboard.service.beans.TagDetails;
import com.heal.dashboard.service.beans.TagMapping;
import com.heal.dashboard.service.beans.UserAccessAccountsBean;
import com.heal.dashboard.service.beans.UserAccessBean;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.TagsDao;
import com.heal.dashboard.service.dao.mysql.UserAttributeDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserPreferencesBL implements BusinessLogic<String, TagMapping, List<TagMapping>> {

    @Autowired
    AccountDao accountdao;
    @Autowired
    UserAttributeDao userAttributeDao;
    @Autowired
    TagsDao tagDao;

    @Override
    public UtilityBean<String> clientValidation(Object requestBody, String... requestParams) throws ClientException {
        String jwtToken = requestParams[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }
        String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        String identifier = requestParams[1];
        if (null == identifier || identifier.trim().isEmpty()) {
            throw new ClientException(Constants.ERROR_INVALID_INPUT_PARAM);
        }

       
        return UtilityBean.<String>builder()
                .accountIdentifier(identifier).authToken(userId)
                .build();
    }

    @Override
    public TagMapping serverValidation(UtilityBean<String> utilityBean) throws ServerException {        
    	   AccountBean accountBean = accountdao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
           if (accountBean == null) {
               log.error("Invalid account identifier. Details: [{}] is unavailable", utilityBean.getAccountIdentifier());
               throw new ServerException("Invalid account identifier");
           }
           TagMapping tagmapping =  getTagMappingForPreferences(accountBean,utilityBean.getAccountIdentifier());

          return tagmapping;
    }

    @Override
    public List<TagMapping> process(TagMapping tagmapping) throws DataProcessingException {
    	  List<TagMapping> userPreferencesDb = tagDao.getTagMappingDetails(tagmapping.getTagId(),tagmapping.getObjectId(),tagmapping.getObjectRefTable(),tagmapping.getAccountId());
          List<TagMapping> userPreferences = new ArrayList<>();
          for (TagMapping tag : userPreferencesDb) {
        	  TagMapping userPreferencePojo = new TagMapping();
              userPreferencePojo.setId(tag.getId());
              userPreferencePojo.setTagKey(tag.getTagKey());
              userPreferencePojo.setTagValue(tag.getTagValue());              userPreferences.add(userPreferencePojo);
          }
          return userPreferences;
    }
    
    
    private TagMapping getTagMappingForPreferences( AccountBean accountBean,String userIdentifier ) throws ServerException   {
        int userId = userAttributeDao.getUserAttributeId(userIdentifier);
        if(userId == 0) {
            log.error(Constants.USER_NOT_EXISTS + ": {0}", userId);
            throw new ServerException(Constants.USER_NOT_EXISTS);
        }
        TagMapping tagMappingDetails = new TagMapping();
        tagMappingDetails.setAccountId(accountBean.getAccountId());
        tagMappingDetails.setUserDetailsId(userIdentifier);
        tagMappingDetails.setObjectId(userId);
        TagDetails tagDetail = tagDao.getTagDetails(Constants.USER_PREFERENCES_TAG_NAME,Constants.DEFAULT_ACCOUNT_ID);
        if(tagDetail!=null) {
            tagMappingDetails.setTagId(tagDetail.getId());
        }
        tagMappingDetails.setObjectRefTable(Constants.USER_ATTRIBUTES_TABLE_NAME_MYSQL);
        return tagMappingDetails;
    }
}
