package com.heal.dashboard.service.businesslogic;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.datastax.driver.core.Row;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.ApplicationHealthDetail;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.SignalType;
import com.heal.dashboard.service.beans.TagMapping;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.UtilityBean;
import com.heal.dashboard.service.beans.ViewApplicationServiceMappingBean;
import com.heal.dashboard.service.dao.mysql.AccountCassandraDao;
import com.heal.dashboard.service.dao.mysql.AccountDao;
import com.heal.dashboard.service.dao.mysql.ControllerDao;
import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.dao.mysql.TagsDao;
import com.heal.dashboard.service.exception.ClientException;
import com.heal.dashboard.service.exception.DataProcessingException;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.DateUtil;
import com.heal.dashboard.service.util.UserValidationUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ApplicationHealthBL implements BusinessLogic<String, UserAccessDetails, List<ApplicationHealthDetail>> {

    @Autowired
    AccountDao accountDao;
    @Autowired
    AccountCassandraDao accountCassandraDao;
    @Autowired
    TagsDao tagDao;
    @Autowired
    ControllerDao controllerDao;
    @Autowired
    MaintainanceWindowsBL maintainanceWindowsBL;
    @Autowired
    MasterDataDao masterDataDao;
    @Autowired
    UserValidationUtil userValidationUtil;

    private int accountId;
    private long toTime;
    private String accountIdentifier;

    @Override
    public UtilityBean<String> clientValidation(Object body, String... requestParams) throws ClientException {
        String identifier = requestParams[0];
        if (null == identifier || identifier.trim().isEmpty()) {
            throw new ClientException("identifier cannot be null or empty");
        }

        String userId = requestParams[2];
        if (null == userId || userId.trim().isEmpty()) {
            throw new ClientException("Authorization token cannot be null or empty");
        }

        String toTimeString = requestParams[1];
        if (null == toTimeString || toTimeString.trim().isEmpty()) {
            throw new ClientException("toTimeString cannot be null or empty");
        }

        return UtilityBean.<String>builder().authToken(userId)
                .accountIdentifier(identifier).pojoObject(toTimeString)
                .build();
    }

    @Override
    public UserAccessDetails serverValidation(UtilityBean<String> utilityBean) throws ServerException {
        toTime = Long.parseLong(utilityBean.getPojoObject());

        AccountBean accountBean = accountDao.getAccountDetailsForIdentifier(utilityBean.getAccountIdentifier());
        if (accountBean == null) {
            log.error("Invalid account identifier. Details: [{}] is unavailable", utilityBean.getAccountIdentifier());
            throw new ServerException("Invalid account identifier");
        }
        accountIdentifier = accountBean.getIdentifier();
        accountId = accountBean.getId();

        UserAccessDetails userAccessDetails = userValidationUtil.getUserAccessDetails(utilityBean.getAuthToken(), utilityBean.getAccountIdentifier());
        if (userAccessDetails == null) {
            log.error("User access details unavailable for user [{}]", utilityBean.getAuthToken());
            throw new ServerException("User access details unavailable");
        }

        List<String> accessibleApplications = userAccessDetails.getApplicationIdentifiers();
        if (accessibleApplications == null || accessibleApplications.isEmpty()) {
            log.error("No applications mapped to user [{}]", utilityBean.getAuthToken());
            throw new ServerException("No applications mapped to user " + utilityBean.getAuthToken());
        }

        return userAccessDetails;
    }

    @Override
    public List<ApplicationHealthDetail> process(UserAccessDetails userAccessDetails) throws DataProcessingException {
        long fromTime = toTime - TimeUnit.MINUTES.toMillis(Long.parseLong(Constants.SIGNAL_CLOSE_WINDOW_TIME));

        long time;
        try {
            List<Row> problemList = getProblemList(accountIdentifier, fromTime, toTime);

            List<String> accessibleApplications = userAccessDetails.getApplicationIdentifiers();

            List<ApplicationHealthDetail> appHealthData = getOpenProblems(accountId, problemList, userAccessDetails.getApplicationServiceMappingBeans());

            if (accessibleApplications.size() != appHealthData.size()) {
                time = System.currentTimeMillis();
                List<String> healthAppIds = appHealthData.parallelStream()
                        .map(ApplicationHealthDetail::getIdentifier).collect(Collectors.toList());

                userAccessDetails.getApplicationServiceMappingBeans().forEach(c -> {
                    if (!healthAppIds.contains(c.getApplicationIdentifier())) {
                        ApplicationHealthDetail detail = new ApplicationHealthDetail();
                        detail.setId(c.getApplicationId());
                        detail.setIdentifier(c.getApplicationIdentifier());
                        detail.setName(c.getApplicationName());
                        appHealthData.add(detail);
                    }
                });

                log.trace("Time taken to add delta applications without any services: {}", System.currentTimeMillis() - time);
            }

            List<TagMapping> tagsForExternalDashboard = tagDao.getApplicationsForDashboardUI(userAccessDetails.getApplicationIdentifiers());
            appHealthData.forEach(app -> {
                Optional<TagMapping> isTagPresent = tagsForExternalDashboard.parallelStream().filter(t -> t.getObjectId() == app.getId()).findAny();
                isTagPresent.ifPresent(tagMapping -> app.setDashboardUId(tagMapping.getTagValue()));
            });

            return appHealthData;
        } catch (Exception e) {
            log.error("Exception encountered while populating application health details. Reason: ", e);
            throw new DataProcessingException("Error in ApplicationHealthBL class while parsing the number  : " + e.getMessage());
        }
    }

    public List<Row> getProblemList(String accountIdentifier, long fromTime, long toTime) throws ServerException {
        List<Row> signalList = null;
        Set<String> signalIds = accountCassandraDao.getSignalId(accountIdentifier, fromTime, toTime);
        if (signalIds != null) {
            signalList = accountCassandraDao.getSignalList(signalIds);
        }
        return signalList;
    }

    public List<ApplicationHealthDetail> getOpenProblems(int accountId, List<Row> signalsRaw, List<ViewApplicationServiceMappingBean> accessibleApplicationList)
            throws ParseException, ServerException {

        long time = System.currentTimeMillis();

        Map<String, Set<Integer>> serviceIdentifierVsAppMapping = accessibleApplicationList.parallelStream()
                .collect(Collectors.groupingBy(ViewApplicationServiceMappingBean::getServiceIdentifier,
                        Collectors.mapping(ViewApplicationServiceMappingBean::getApplicationId, Collectors.toSet())));

        Map<Integer, ApplicationHealthDetail> dataMap = initializeHealthData(accessibleApplicationList);

        if (signalsRaw != null) {
            // In case of batch jobs, application identifiers will be present in service identifier column in cassandra for signal details.
            // Hence, adding application identifiers as well in this map.
            serviceIdentifierVsAppMapping.putAll(controllerDao.getApplicationList(accountId).parallelStream()
                    .collect(Collectors.groupingBy(Controller::getIdentifier,
                            Collectors.mapping(c -> Integer.parseInt(c.getAppId()), Collectors.toSet()))));

            signalsRaw.forEach(signal -> {
                if (isProblemOpen(signal) && !SignalType.INFO.name().equalsIgnoreCase(signal.getString("signal_type"))) {
                    String problemId = signal.getString("signal_id");
                    Set<String> affectedServices = signal.getSet("service_ids", String.class);
                    String signalType = signal.getString("signal_type");

                    if (problemId == null || affectedServices == null) {
                        log.warn("Invalid fields found in problem. Details: signal_id is [{}] and service_ids are [{}] in " +
                                        "signal_details table.", problemId, affectedServices);
                        return;
                    }

                    Set<Integer> affectedAppIds =  affectedServices.parallelStream()
                            .map(service -> serviceIdentifierVsAppMapping.getOrDefault(service, new HashSet<>()))
                            .flatMap(Collection::stream)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    addProblemData(dataMap, affectedAppIds, signal.getString("severity"), signalType);
                }
            });
            log.debug("Time taken to go through signals rows: {}", System.currentTimeMillis() - time);
        }

        return new ArrayList<>(dataMap.values());
    }

    public Map<Integer, ApplicationHealthDetail> initializeHealthData(List<ViewApplicationServiceMappingBean> accessibleApplicationList)
            throws ParseException {

        Map<Integer, List<ViewApplicationServiceMappingBean>> appIdVsServiceIdentifiers = accessibleApplicationList.parallelStream()
                .collect(Collectors.groupingBy(ViewApplicationServiceMappingBean::getApplicationId));

        Timestamp date = new Timestamp(DateUtil.getDateInGMT(System.currentTimeMillis()).getTime());

        return appIdVsServiceIdentifiers.entrySet().parallelStream().map(entry -> {
            int appId = entry.getKey();
            List<ViewApplicationServiceMappingBean> serviceIdentifiers = entry.getValue();

            ApplicationHealthDetail temp = new ApplicationHealthDetail();

            boolean isWindow = serviceIdentifiers.parallelStream().allMatch(service -> maintainanceWindowsBL
                    .getServiceMaintenanceStatus(service.getServiceIdentifier(), date));
            temp.setMaintenanceWindowStatus(isWindow);
            temp.setName(serviceIdentifiers.get(0).getApplicationName());
            temp.setIdentifier(serviceIdentifiers.get(0).getApplicationIdentifier());
            temp.setId(appId);
            temp.setApplicationHealthStatus();

            return temp;
        }).collect(Collectors.toMap(ApplicationHealthDetail::getId, Function.identity()));
    }

    public boolean isProblemOpen(Row signal) {
        String status = signal.getString("current_status");
        String problemId = signal.getString("signal_id");

        if ("open".equalsIgnoreCase(status)) {
            log.debug("Problem: {} is open.", problemId);
            return true;
        } else {
            log.debug("Problem: {} is {}.", problemId, status);
            return false;
        }
    }

    public void addProblemData(Map<Integer, ApplicationHealthDetail> data, Set<Integer> affectedAppIds,
                               String signalSeverity, String signalType) {

        String severityTypeStr = masterDataDao.getSubTypeNameForSubTypeId(Integer.parseInt(signalSeverity));
        if(severityTypeStr == null) {
            log.error("Signal severity viewTypes unavailable for subTypeId [{}]", signalSeverity);
            return;
        }

        List<ApplicationHealthDetail> applicationHealthDetails = affectedAppIds.parallelStream().map(data::get)
                .filter(Objects::nonNull).collect(Collectors.toList());

        applicationHealthDetails.forEach(temp -> {
            if (Constants.PROBLEM_LITERAL.equalsIgnoreCase(signalType)) {
                temp.getProblem().parallelStream().filter(p -> p.getName().equals(severityTypeStr))
                        .collect(Collectors.toList())
                        .forEach(p -> {
                            int currCount = p.getCount();
                            p.setCount(currCount + 1);
                        });
            } else if (Constants.BATCH_JOB_LITERAL.equalsIgnoreCase(signalType)) {
                temp.getBatch().parallelStream().filter(b -> b.getName().equals(severityTypeStr))
                        .collect(Collectors.toList())
                        .forEach(b -> {
                            int currCount = b.getCount();
                            b.setCount(currCount + 1);
                        });
            } else {
                temp.getWarning().parallelStream().filter(w -> w.getName().equals(severityTypeStr))
                        .collect(Collectors.toList())
                        .forEach(w -> {
                            int currCount = w.getCount();
                            w.setCount(currCount + 1);
                        });
            }
        });
    }
}
