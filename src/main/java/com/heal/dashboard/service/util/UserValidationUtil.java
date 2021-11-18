package com.heal.dashboard.service.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.heal.dashboard.service.beans.*;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserValidationUtil {

    @Autowired
    private ControllerDao controllerDao;

    @Autowired
    private AccountDao accountDao;

    private static final Gson gson = new GsonBuilder().create();

    public UserAccessDetails getUserAccessDetails(String userIdentifier, String accountIdentifier) {
        UserAccessBean accessDetails = accountDao.fetchUserAccessDetailsUsingIdentifier(userIdentifier);

        if (accessDetails == null) {
            log.error("User access bean unavailable for user [{}] and account [{}]", userIdentifier, accountIdentifier);
            return null;
        }

        UserAccessDetails userAccessDetails = getUserAccessibleApplicationsServices(accessDetails.getAccessDetails(), userIdentifier, accountIdentifier);

        if (userAccessDetails == null) {
            log.error("User access details unavailable for user [{}] and account [{}]", userIdentifier, accountIdentifier);
        }

        return userAccessDetails;
    }

    private UserAccessDetails getUserAccessibleApplicationsServices(String accessDetails, String userIdentifier, String accountIdentifier) {
        UserAccessDetails userAccessDetails = null;

        Type userBeanType = new TypeToken<AccessDetailsBean>() {
        }.getType();

        AccessDetailsBean bean = gson.fromJson(accessDetails, userBeanType);

        if (bean != null && bean.getAccounts() != null) {
            if (bean.getAccounts().contains(accountIdentifier)) {
                AccountBean accessibleAccount = accountDao.getAccountDetailsForIdentifier(accountIdentifier);

                Map<String, AccessDetailsBean.Application> accessibleApplications = bean.getAccountMapping();

                if (accessibleApplications == null || accessibleApplications.isEmpty()) {
                    log.error("There no applications mapped to account [{}] and user [{}]",
                            accountIdentifier, userIdentifier);
                    return null;
                }

                AccessDetailsBean.Application applicationIdentifiers = accessibleApplications.get(accountIdentifier);
                List<Controller> applicationControllerList = controllerDao.getApplicationsForAccount(accessibleAccount.getId());

                if (applicationIdentifiers.getApplications().contains("*")) {
                    userAccessDetails = populateUserAccessDetails(accessibleAccount.getId(), applicationControllerList);
                } else {
                    applicationControllerList = applicationControllerList.parallelStream()
                            .filter(app -> (applicationIdentifiers.getApplications().contains(app.getIdentifier())))
                            .collect(Collectors.toList());

                    userAccessDetails = populateUserAccessDetails(accessibleAccount.getId(), applicationControllerList);
                }
            } else if (bean.getAccounts().contains("*")) {
                AccountBean accessibleAccount = accountDao.getAccountDetailsForIdentifier(accountIdentifier);

                List<Controller> applicationControllerList = controllerDao.getApplicationsForAccount(accessibleAccount.getId());

                userAccessDetails = populateUserAccessDetails(accessibleAccount.getId(), applicationControllerList);
            }
        }

        return userAccessDetails;
    }

    private UserAccessDetails populateUserAccessDetails(int accountId, List<Controller> applicationControllerList) {
        UserAccessDetails userAccessDetails = new UserAccessDetails();
        userAccessDetails.setApplicationIds(new ArrayList<>());
        userAccessDetails.setServiceIds(new ArrayList<>());
        userAccessDetails.setServiceIdentifiers(new ArrayList<>());
        userAccessDetails.setTransactionIds(new ArrayList<>());
        userAccessDetails.setAgents(new ArrayList<>());
        userAccessDetails.setApplicationIdentifiers(new ArrayList<>());

        if(applicationControllerList.isEmpty()) {
            return userAccessDetails;
        }

        List<ViewApplicationServiceMappingBean> beans = getAppServiceMappingDetails(accountId, applicationControllerList);
        userAccessDetails.setApplicationServiceMappingBeans(beans);
        userAccessDetails.setApplicationIds(beans.parallelStream().map(ViewApplicationServiceMappingBean::getApplicationId).distinct().collect(Collectors.toList()));
        userAccessDetails.setApplicationIdentifiers(beans.parallelStream().map(ViewApplicationServiceMappingBean::getApplicationIdentifier).distinct().collect(Collectors.toList()));
        userAccessDetails.setServiceIds(beans.parallelStream().map(ViewApplicationServiceMappingBean::getServiceId).filter(serviceId -> serviceId > 0).distinct().collect(Collectors.toList()));
        userAccessDetails.setServiceIdentifiers(beans.parallelStream().map(ViewApplicationServiceMappingBean::getServiceIdentifier).filter(s -> !StringUtils.isEmpty(s)).distinct().collect(Collectors.toList()));
        return userAccessDetails;
    }

    private List<ViewApplicationServiceMappingBean> getAppServiceMappingDetails(int accountId, List<Controller> applicationControllerList) {
        if(applicationControllerList.isEmpty()) {
            return Collections.emptyList();
        }

        return applicationControllerList.parallelStream()
                .map(controller -> {
                    List<ViewApplicationServiceMappingBean> services = controllerDao.getServicesForApplication(accountId, controller.getIdentifier());
                    if(!services.isEmpty()) {
                        return services;
                    }
                    return Collections.singleton(ViewApplicationServiceMappingBean.builder()
                            .applicationId(Integer.parseInt(controller.getAppId()))
                            .applicationIdentifier(controller.getIdentifier())
                            .applicationName(controller.getName())
                            .build());
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
