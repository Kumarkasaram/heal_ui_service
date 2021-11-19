package com.heal.dashboard.service.businesslogic;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.heal.dashboard.service.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.pojo.ApplicationDetails;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.TxnAndGroupBean;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.ViewApplicationServiceMappingBean;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.TransactionDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.UserValidationUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GetApplicationBL implements BusinessLogic<String, UserAccessDetails, List<ApplicationDetails>> {

    @Autowired
    private AccountDao accountdao;
    @Autowired
    private TransactionDao transactionDao;
    @Autowired
    private ControllerDao controllerDao;
    @Autowired
    private UserValidationUtil userValidationUtil;

    private int accountId;

    @Override
    public UtilityBean<String> clientValidation(Object requestObject, String... requestParams) throws ClientException {
        String jwtToken = requestParams[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }
        String identifier = requestParams[1];
        if (null == identifier || identifier.trim().isEmpty()) {
            throw new ClientException("identifier cant be null or empty");
        }

        //String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        String userId = jwtToken;
        if (null == userId || userId.trim().isEmpty()) {
            throw new ClientException("User details extraction failure");
        }

        return UtilityBean.<String>builder()
                .authToken(userId)
                .accountIdentifier(identifier)
                .build();
    }

    @Override
    public UserAccessDetails serverValidation(UtilityBean<String> utilityBean) throws ServerException {
        AccountBean accountBean = accountdao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
        if (accountBean == null) {
            log.error("Invalid account identifier. Details: [{}] is unavailable", utilityBean.getAccountIdentifier());
            throw new ServerException("Invalid account identifier");
        }

        accountId = accountBean.getId();

        UserAccessDetails userAccessDetails = userValidationUtil.getUserAccessDetails(utilityBean.getAuthToken(), accountBean.getIdentifier());
        if(userAccessDetails == null) {
            log.error("User access details unavailable for user [{}]", utilityBean.getAuthToken());
            throw new ServerException("User access details unavailable");
        }

        List<String> userApps = userAccessDetails.getApplicationIdentifiers();
        if (userApps == null || userApps.isEmpty()) {
            log.error("No applications mapped to user [{}]", utilityBean.getAuthToken());
            throw new ServerException("No applications mapped to user " + utilityBean.getAuthToken());
        }

        List<Controller> accountWiseApps = controllerDao.getApplicationsForAccount(accountBean.getId());
        if (accountWiseApps.isEmpty()) {
            log.error("Applications unavailable for account [{}]", accountBean.getIdentifier());
            throw new ServerException("Application unavailable for account " + accountBean.getIdentifier());
        }

        List<Controller> accessibleApps = accountWiseApps.parallelStream().filter(app -> userApps.contains(app.getIdentifier())).collect(Collectors.toList());

        if (accessibleApps.isEmpty()) {
            log.error("Applications mapped to user [{}] is unavailable in account [{}]", utilityBean.getAuthToken(), accountBean.getIdentifier());
            throw new ServerException(String.format("Applications mapped to user [%s] is unavailable in account [%s]", utilityBean.getAuthToken(), accountBean.getIdentifier()));
        }

        return userAccessDetails;
    }

    @Override
    public List<ApplicationDetails> process(UserAccessDetails userAccessDetails) throws DataProcessingException {
        Map<String, List<ViewApplicationServiceMappingBean>> appToServicesMapping = userAccessDetails.getApplicationServiceMappingBeans()
                .parallelStream().collect(Collectors.groupingBy(ViewApplicationServiceMappingBean::getApplicationIdentifier));

        return appToServicesMapping.entrySet().parallelStream().map(entry -> {
            ViewApplicationServiceMappingBean bean = entry.getValue().get(0);

            ApplicationDetails applicationDetails = new ApplicationDetails();
            applicationDetails.setId(bean.getApplicationId());
            applicationDetails.setName(bean.getApplicationName());
            applicationDetails.setIdentifier(bean.getApplicationIdentifier());

            boolean txnEnabled = entry.getValue().parallelStream().anyMatch(v -> {
                List<TxnAndGroupBean> appTxnDetails = transactionDao.getTxnAndGroupForServiceAndAccount(accountId, v.getServiceId());
                if (!appTxnDetails.isEmpty()) {
                    return appTxnDetails.parallelStream().anyMatch(it -> it.getIsBusinessTransaction() == 1);
                }
                return false;
            });

            applicationDetails.setHasTransactionConfigured(txnEnabled);

            return applicationDetails;
        }).sorted(java.util.Comparator.comparing(ApplicationDetails::isHasTransactionConfigured, java.util.Comparator.reverseOrder()).
                thenComparing(ApplicationDetails::getName)).collect(Collectors.toList());
    }
}

