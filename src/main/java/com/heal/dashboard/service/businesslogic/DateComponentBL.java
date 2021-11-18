package com.heal.dashboard.service.businesslogic;

import com.heal.dashboard.service.beans.DateComponentBean;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.FeaturesDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.exception.UiServiceException;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DateComponentBL implements BusinessLogic<String, List<DateComponentBean>, List<DateComponentBean>> {

    @Autowired
    private FeaturesDao featuresDao;

    @Override
    public UtilityBean<String> clientValidation(Object body, String... params) throws ClientException {
        String jwtToken = params[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }

        String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        if (null == userId || userId.trim().isEmpty()) {
            throw new ClientException("User details extraction failure");
        }

        return UtilityBean.<String>builder().authToken(userId).build();
    }

    @Override
    public List<DateComponentBean> serverValidation(UtilityBean<String> utilityBean) throws ServerException {
        return null;
    }

    @Override
    public List<DateComponentBean> process(List<DateComponentBean> bean) throws DataProcessingException {
		List<DateComponentBean> dateComponentBeans;
		try {
			dateComponentBeans = featuresDao.getDateTimeDropdownList();
		} catch (UiServiceException e) {
			throw new DataProcessingException(e.getMessage());
		}

		if(dateComponentBeans.isEmpty()) {
			log.error("Date components unavailable");
			throw new DataProcessingException("Date components unavailable");
		}

		return dateComponentBeans;
    }
}
