package com.heal.dashboard.service.businesslogic;

import com.heal.dashboard.service.beans.*;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ComponentInstanceDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CommonServiceBLUtil;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ClusterKpiMappingServiceBL implements BusinessLogic<Map<String,Object>,UtilityBean<Map<String, Object>>,Object> {
    @Autowired
    AccountDao accountDao;
    @Autowired
    MasterDataDao masterDataService;
    @Autowired
    CommonServiceBLUtil commonServiceBLUtil;
    @Autowired
    ComponentInstanceDao componentInstanceDao;
    @Override
    public UtilityBean<Map<String,Object>> clientValidation(Object requestBody, String... requestParams) throws ClientException {
        String jwtToken = requestParams[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }
        String identifier = requestParams[1];
        if (null == identifier || identifier.trim().isEmpty()) {
            throw new ClientException("identifier cant be null or empty");
        }
        String instanceId = requestParams[2];
        if (!validateParam(instanceId)) {
            throw new ClientException("instanceId cant be null or empty");
        }
        String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        if (null == userId || userId.trim().isEmpty()) {
            throw new ClientException("User details extraction failure");
        }
        Map<String,Object> tempMap = new HashMap<>();
        tempMap.put("instanceId",instanceId);

        return UtilityBean.<Map<String,Object> >builder().authToken(userId).accountIdentifier(identifier).pojoObject(tempMap)
                .build();
    }

    @Override
    public UtilityBean<Map<String, Object>> serverValidation(UtilityBean<Map<String,Object>> utilityBean) throws ServerException {
        Map<String,Object> tempMap = new HashMap<>();
        AccountBean accountBean = accountDao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
        if (accountBean == null) {
            log.error("Invalid account identifier. Details: [{}] is unavailable", utilityBean.getAccountIdentifier());
            throw new ServerException("Invalid account identifier");
        }
        AllAccountDetails allAccountDetails = commonServiceBLUtil.loadAccountDetails(accountBean.getAccountId());
        tempMap.put("accountBean",accountBean);
        tempMap.put("allAccountDetails",allAccountDetails);
        tempMap.put("instanceId",utilityBean.getPojoObject().get("instanceId"));
        return UtilityBean.<Map<String,Object> >builder().pojoObject(tempMap)
                .build();
    }

    @Override
    public Map<String,String> process(UtilityBean<Map<String,Object> > utilityBean) throws DataProcessingException {
        AccountBean accountBean = (AccountBean) utilityBean.getPojoObject().get("accountBean");
       // CompInstClusterDetails compInstClusterDetails = (CompInstClusterDetails) utilityBean.getPojoObject().get("instanceDetails");
        Map<String,String> result = new HashMap<>();
        AllAccountDetails allAccountDetails = (AllAccountDetails) utilityBean.getPojoObject().get("allAccountDetails");
        CompInstClusterDetails instanceDetails = allAccountDetails.getCompInstanceDetailsList().stream()
                .filter( it -> ( it.getInstanceId() == Integer.parseInt(utilityBean.getPojoObject().get("instanceId").toString())))
                .findAny()
                .orElse(null);

        if( instanceDetails == null)    {
            throw new DataProcessingException("invalid instanceID");
        }
        result =getAttributesForInstance(accountBean,instanceDetails);

        return result;
    }


    private  Map<String,String> getAttributesForInstance(AccountBean account, CompInstClusterDetails instanceDetails) {
        log.trace(Constants.INVOKED_METHOD+"getAttributesForInstance account: {}, instance: {}.",
                account.getAccountId(),instanceDetails.getInstanceId());
        Map<String,String> result = new HashMap<>();

        try {
            List<CompInstanceAttributesBean> attributesViewBeanList = componentInstanceDao.getCompInstForAccountComInstName(instanceDetails.getInstanceName().trim(),
                            account.getAccountId()).getAttributes();


            //Mandatory fields are manually put into the attribute map
            result.put(Constants.COMPONENT_KEY, instanceDetails.getComponentName());
            result.put(Constants.COMPONENT_TYPE_KEY, instanceDetails.getComponentTypeName());
            result.put(Constants.COMPONENT_VERSION_KEY, instanceDetails.getComponentVersionName());
            result.put(Constants.COMPONENT_ID_KEY, String.valueOf(instanceDetails.getCompId()));

            for(CompInstanceAttributesBean attr: attributesViewBeanList)    {

                // removing mandatory filed check as part of CC-472
                if(attr.getIsUiVisible() == 1)  {
                    result.put(attr.getDisplayName(),attr.getAttributeValue());
                }

            }
        }   catch (Exception e) {
            log.error("Error occurred while fetching attributes for instance: {}",instanceDetails.getInstanceId(),e);
        }
        return result;
    }



    private static boolean validateParam(String instanceId) {
        try {
            int i = Integer.valueOf(instanceId);
        }   catch (NumberFormatException ne)    {
            log.error("Invalid number received as input.",ne);
            return false;
        }
        return true;
    }


}
