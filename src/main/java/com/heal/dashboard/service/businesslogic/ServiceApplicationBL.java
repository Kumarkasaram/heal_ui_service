package com.heal.dashboard.service.businesslogic;

import com.heal.dashboard.service.beans.*;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.TagsDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.pojo.ApplicationDetails;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class ServiceApplicationBL implements  BusinessLogic<String,UtilityBean<String>,Set<ApplicationDetails>> {

    @Autowired
    AccountDao  accountdao;
    @Autowired
    ControllerDao controllerDao;
    @Autowired
    TagsDao tagsDao;

    @Override
    public UtilityBean<String> clientValidation(Object requestBody, String... requestParams) throws ClientException {
        String jwtToken = requestParams[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }

        String accountIdentifier = requestParams[1];
        if (null == accountIdentifier || accountIdentifier.trim().isEmpty()) {
            throw new ClientException("identifier cant be null or empty");
        }

        String serviceId = requestParams[2];
        if (null == serviceId || serviceId.trim().isEmpty()) {
            throw new ClientException("serviceId cant be null or empty");
        }
        String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        if (null == userId || userId.trim().isEmpty()) {
            throw new ClientException("User details extraction failure");
        }

        return UtilityBean.<String>builder()
                .authToken(userId)
                .accountIdentifier(accountIdentifier)
                .pojoObject(serviceId)
                .build();
    }


    @Override
    public UtilityBean<String> serverValidation(UtilityBean<String> utilityBean) throws ServerException {
        AccountBean accountBean = accountdao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
        if (accountBean == null) {
            log.error("Invalid account identifier. Details: [{}] is unavailable", utilityBean.getAccountIdentifier());
            throw new ServerException("Invalid account identifier");
        }
        String serviceIdentifier = getServiceIdentifier(accountBean.getAccountId(),Integer.parseInt(utilityBean.getPojoObject()));
        if ("".equals(serviceIdentifier)) {
            String err = Constants.MESSAGE_INVALID_SERVICE + " " + utilityBean.getPojoObject();
            log.error(err);
            throw new ServerException(Constants.MESSAGE_INVALID_SERVICE);
        }
        return UtilityBean.<String>builder()
                .accountIdentifier(String.valueOf(accountBean.getAccountId()))
                .pojoObject(serviceIdentifier)
                .build();
    }

    @Override
    public Set<ApplicationDetails> process(UtilityBean<String> key) throws DataProcessingException {
        Set<ApplicationDetails> applications = controllerDao.getApplicationsByServiceId(Integer.parseInt(key.getPojoObject()))
                .parallelStream().filter(app -> app.getAccountId() == Integer.parseInt(key.getAccountIdentifier())).map(app ->
                        ApplicationDetails.builder().id(app.getId()).name(app.getName()).identifier(app.getIdentifier()).build())
                .collect(Collectors.toSet());

        int tagId = tagsDao.getTagDetails(Constants.DASHBOARD_UID_TAG,Constants.DEFAULT_ACCOUNT_ID).getId();

        try {
            List<TagMapping> tags = tagsDao.getTagMappingDetailsByAccountId(Integer.parseInt(key.getAccountIdentifier())).parallelStream()
                    .filter(tag -> tag.getTagId() == tagId && tag.getObjectRefTable().equals(Constants.CONTROLLER))
                    .collect(toList());

            applications.forEach(app -> {
                Optional<TagMapping> tag = tags.parallelStream().filter(t -> t.getObjectId() == app.getId()).findAny();
                tag.ifPresent(tagMappingDetails -> app.setDashboardUId(tagMappingDetails.getTagValue()));
            });
        } catch (Exception ex) {
            throw new DataProcessingException("Error while fetching TagMappingDetailsByAccountId information for account Id : " + key.getAccountIdentifier());

        }
        return applications;
    }


    private String  getServiceIdentifier (int  accountId, int  serviceId) {
        try {
            Controller controller = controllerDao.getControllerListByAccountIdAndApplicationId(accountId,serviceId);
            if(controller!=null)   {
                return controller.getIdentifier();
            }

            }   catch (Exception e) {
                log.error("Error occurred while validating service id.", e);
            }
                return "";
        }
}
