package com.heal.dashboard.service.businesslogic;


import java.util.List;
import java.util.stream.Collectors;

import com.heal.dashboard.service.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.AccountMappingBean;
import com.heal.dashboard.service.beans.UserAccessAccountsBean;
import com.heal.dashboard.service.beans.UserAccessBean;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GetAccountsBL implements BusinessLogic<String, UserAccessAccountsBean, List<AccountBean>> {

    @Autowired
    AccountDao accountdao;

    @Override
    public UtilityBean<String> clientValidation(Object requestBody, String... requestParams) throws ClientException {
        String jwtToken = requestParams[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }

        String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        if (null == userId || userId.trim().isEmpty()) {
            log.error("Invalid JWT. User identifier extraction failed.");
            throw new ClientException("User details extraction failure");
        }

        return UtilityBean.<String>builder()
                .pojoObject(userId)
                .build();
    }

    @Override
    public UserAccessAccountsBean serverValidation(UtilityBean<String> utilityBean) throws ServerException {
        String userId = utilityBean.getPojoObject();

        UserAccessBean userAccessDetails = accountdao.fetchUserAccessDetailsUsingIdentifier(userId);
        if (null == userAccessDetails || null == userAccessDetails.getAccessDetails()) {
            log.error("Invalid user access details. Details: Required access details for user [{}] is unavailable", userId);
            throw new ServerException("Invalid user access details");
        }

        AccountMappingBean accountMappingBean;
        try {
            accountMappingBean = new Gson().fromJson(userAccessDetails.getAccessDetails(), AccountMappingBean.class);
        } catch (JsonSyntaxException exception) {
            log.error("Invalid user account mapping information. Details: ", exception);
            throw new ServerException("User access details JSON to POJO conversion failure");
        }

        List<AccountBean> accessibleAccounts = accountdao.getAccountDetails(Constants.TIME_ZONE_TAG, Constants.ACCOUNT_TABLE_NAME_MYSQL_DEFAULT);
        if (null == accessibleAccounts) {
            log.error("Account information unavailable");
            throw new ServerException("Account information unavailable");
        }

        return new UserAccessAccountsBean(accountMappingBean, accessibleAccounts);
    }

    @Override
    public List<AccountBean> process(UserAccessAccountsBean bean) throws DataProcessingException {
        AccountMappingBean accountMappingBean = bean.getAccountMappingBean();
        List<AccountBean> accounts = bean.getAccounts();
        List<String> accountIdentifiers = accountMappingBean.getAccounts();

        if (!accountIdentifiers.contains("*")) {
            accounts = accounts.parallelStream()
                    .filter(acc -> accountIdentifiers.contains(acc.getIdentifier()))
                    .collect(Collectors.toList());
        }

        return accounts;
    }
}
