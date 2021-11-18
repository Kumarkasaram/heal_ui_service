package com.heal.dashboard.service.businesslogic;

import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import org.springframework.stereotype.Component;

@Component
public interface BusinessLogic<T, V, R> {
    UtilityBean<T> clientValidation(Object requestBody, String... requestParams) throws ClientException;
    V serverValidation(UtilityBean<T> utilityBean) throws ServerException;
    R process(V bean) throws DataProcessingException;
}
