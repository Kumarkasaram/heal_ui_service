package com.heal.dashboard.service.businesslogic;

import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.FileSummaryDetailsBean;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CommonUtils;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileUploadServiceBL implements BusinessLogic<String,List<FileSummaryDetailsBean>,Map<String, List<String>>> {
    @Autowired
    AccountDao accountDao;
    @Autowired
    MasterDataDao masterDataDao;

    @Override
    public UtilityBean<String> clientValidation(Object requestBody, String... requestParams) throws ClientException {
        String jwtToken = requestParams[0];
        if (null == jwtToken || jwtToken.trim().isEmpty()) {
            throw new ClientException(Constants.AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY);
        }
        String identifier = requestParams[1];
        if (null == identifier || identifier.trim().isEmpty()) {
            throw new ClientException("identifier cant be null or empty");
        }

        String userId = CommonUtils.extractUserIdFromJWT(jwtToken);
        if (null == userId || userId.trim().isEmpty()) {
            throw new ClientException("User details extraction failure");
        }
        return UtilityBean.<String >builder().authToken(userId).accountIdentifier(identifier).pojoObject(null)
                .build();
    }

    @Override
    public List<FileSummaryDetailsBean>  serverValidation(UtilityBean<String> utilityBean) throws ServerException {
        AccountBean account = accountDao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
        if (account == null) {
            log.error("Error while fetching account details for identifier [{}]", utilityBean.getAccountIdentifier());
            throw new ServerException("Error while fetching account details for identifier [{}]" + utilityBean.getAccountIdentifier());
        }
        List<FileSummaryDetailsBean> fileSummaryDetailsBeanList = masterDataDao.getFileSummaryDetailsByAccount(account.getId());
        if (fileSummaryDetailsBeanList == null || fileSummaryDetailsBeanList.size()==0) {
            log.error("Account does not have upload details. Account:{}\", identifier", utilityBean.getAccountIdentifier());
            throw new ServerException("Account does not have upload details. Account:{}\", identifier" + utilityBean.getAccountIdentifier());
        }
        return fileSummaryDetailsBeanList;
    }

    @Override
    public Map<String, List<String>> process(List<FileSummaryDetailsBean> fileSummaryDetailsBeanList) throws DataProcessingException {
        Map<Integer, Map<String, String>> dataInMap = null;
        Map<String, List<String>> listMap = new HashMap<>();
        if(fileSummaryDetailsBeanList != null && !fileSummaryDetailsBeanList.isEmpty()) {
            dataInMap = fileSummaryDetailsBeanList.stream()
                    .filter(f -> f.getIsDebugLogs() == 1)
                    .collect(Collectors.groupingBy(FileSummaryDetailsBean::getFileProcessedId,
                            Collectors.toMap(FileSummaryDetailsBean::getKey, FileSummaryDetailsBean::getValue,
                                    (v1, v2) -> v1 == null ? v2:v1)));
        }
        Function<Map.Entry<Integer, Map<String, String>>, Map.Entry<String, List<String>>> externalToMyLocation = e -> {
            List<String> timerange = new ArrayList<>();

            String startTime = (e.getValue().containsKey("TXN_START_TIME") && e.getValue().containsKey("COMP_START_TIME")) ?
                    DateUtil.getLesserValue(e.getValue().get("TXN_START_TIME"), e.getValue().get("COMP_START_TIME")) :
                    (e.getValue().containsKey("TXN_START_TIME")) ? e.getValue().get("TXN_START_TIME") : e.getValue().getOrDefault("COMP_START_TIME", null);
            timerange.add(startTime);
            String endTime = (e.getValue().containsKey("TXN_END_TIME") && e.getValue().containsKey("COMP_END_TIME")) ?
                    DateUtil.getGreaterValue(e.getValue().get("TXN_END_TIME"), e.getValue().get("COMP_END_TIME")) :
                    (e.getValue().containsKey("TXN_END_TIME")) ? e.getValue().get("TXN_END_TIME") : e.getValue().getOrDefault("COMP_END_TIME", null);
            timerange.add(endTime);

            return new AbstractMap.SimpleEntry<>(e.getKey()+"", timerange);
        };

        listMap = dataInMap.entrySet().stream()
                .filter( e -> (e.getValue().containsKey("TXN_START_TIME") && e.getValue().containsKey("TXN_END_TIME"))
                        || (e.getValue().containsKey("COMP_START_TIME") && e.getValue().containsKey("COMP_END_TIME")))
                .map(externalToMyLocation)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return listMap;
    }
}
