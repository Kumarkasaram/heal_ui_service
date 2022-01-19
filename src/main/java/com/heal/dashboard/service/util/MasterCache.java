package com.heal.dashboard.service.util;

import com.appnomic.appsone.api.beans.*;
import com.appnomic.appsone.api.beans.xpt.*;
import com.appnomic.appsone.api.common.Constants;
import com.appnomic.appsone.api.pojo.*;
import com.appnomic.appsone.api.service.mysql.*;
import com.appnomic.appsone.api.util.ConfProperties;
import com.google.common.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Jitendra Kumar : 15/1/19
 */
public class MasterCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterCache.class);

    private static final Integer maxSize = ConfProperties.getInt(Constants.CACHE_MAXIMUM_SIZE_PROPERTY_NAME,
            Constants.CACHE_MAXIMUM_SIZE_DEFAULT_VALUE);
    private static final Integer cacheTimeout = ConfProperties.getInt(Constants.CACHE_TIMEOUT_IN_MINUTES_PROPERTY_NAME,
            Constants.CACHE_TIMEOUT_IN_MINUTES_DEFAULT_VALUE);
    private static MasterCache instance = null;

    private LoadingCache<String, List<ViewCoverageWinProfDetailsBean>> coverageWindowProfileDetails = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterAccess(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<ViewCoverageWinProfDetailsBean>>() {
                @Override
                public List<ViewCoverageWinProfDetailsBean> load(String key) throws Exception {
                    return ThresholdDataService.getCoverageWindowsProfiles();
                }
            });

    private LoadingCache<Integer, List<FlowDetailsBean>> flowDetails = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterAccess(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<FlowDetailsBean>>() {
                @Override
                public List<FlowDetailsBean> load(Integer key) {
                    return XpTDataService.getFlowDetails(key);
                }
            });

    private LoadingCache<Integer, List<StepDetailsBean>> stepDetails = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterAccess(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<StepDetailsBean>>() {
                @Override
                public List<StepDetailsBean> load(Integer key) {
                    return XpTDataService.getStepDetails(key);
                }
            });

    private LoadingCache<Integer, List<EventDetailsBean>> eventDetails = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterAccess(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<EventDetailsBean>>() {
                @Override
                public List<EventDetailsBean> load(Integer key) {
                    return XpTDataService.getEventDetails(key);
                }
            });

    private LoadingCache<Integer, List<EventDetailsBean>> eventDetailsForAFlow = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterAccess(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<EventDetailsBean>>() {
                @Override
                public List<EventDetailsBean> load(Integer key) {
                    return XpTDataService.getEventDetailsForAFlow(key);
                }
            });


    private LoadingCache<Integer, List<FlowSegmentsBean>> flowSegmentsDeatils = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterAccess(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<FlowSegmentsBean>>() {
                @Override
                public List<FlowSegmentsBean> load(Integer flowId) {
                    return XpTDataService.getFlowSegmentsBean(flowId);
                }
            });

    private LoadingCache<Integer, List<StepConnectionDetailsBean>> stepConnectionDetailsBean = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterAccess(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<StepConnectionDetailsBean>>() {
                @Override
                public List<StepConnectionDetailsBean> load(Integer key) {
                    return XpTDataService.getStepConnectionDetailsBean(key);
                }
            });

    private MasterCache() {
    }

    /**
     * Implementing Bill Pugh Singleton Implementation
     * ref: {https://www.journaldev.com/1377/java-singleton-design-pattern-best-practices-examples}
     */
    private static class SingletonHelper {
        private static final MasterCache INSTANCE = new MasterCache();
    }

    public static MasterCache getInstance() {
        return SingletonHelper.INSTANCE;

    }

    /**
     * @return
     */
    public List<Account> getAccounts() {
        try {
            return MasterDataService.getAccountList();
        } catch (Exception e) {

        }
        return null;
    }

    public List<ViewCoverageWinProfDetailsBean> getCoverageWinProfDetailsList(String profileName) {
        try {
            return coverageWindowProfileDetails.get(Constants.ALL_PROFILES).stream()
                    .filter(it -> it.getProfileName().equals(profileName))
                    .collect(Collectors.toList());
        } catch (Exception e) {

        }
        return null;
    }

    public ViewCoverageWinProfDetailsBean getCoverageWinProfDetailsFromId(Integer profileId) {
        try {
            Optional<ViewCoverageWinProfDetailsBean> winProfileBean = coverageWindowProfileDetails.get(Constants.ALL_PROFILES).stream()
                    .filter(it -> it.getProfileId() == profileId)
                    .findFirst();

            if (winProfileBean.isPresent()) return winProfileBean.get();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public List<FlowDetailsBean> getFlowDetailsBeans(Integer accountId) {
        try {
            return flowDetails.get(accountId);
        } catch (Exception e) {

        }
        return null;
    }

    public List<StepDetailsBean> getStepDetailsBeans(Integer flowId) {
        try {
            return stepDetails.get(flowId);
        } catch (Exception e) {

        }
        return null;
    }

    public List<EventDetailsBean> getEventDetailsBeans(Integer stepId) {
        try {
            return eventDetails.get(stepId);
        } catch (Exception e) {

        }
        return null;
    }

    public List<EventDetailsBean> getEventDetailsBeansForAFlow(Integer flowId) {
        try {
            return eventDetailsForAFlow.get(flowId);
        } catch (Exception e) {

        }
        return null;
    }

    public List<FlowSegmentsBean> getFlowSegments(Integer flowId) {
        try{
            return flowSegmentsDeatils.get(flowId);
        }catch (Exception e){
            LOGGER.error("Error occurred while getting flow segment details "+e);
        }
        return null;
    }

    public List<StepConnectionDetailsBean> getStepConnectionDetailsBean(Integer flowId) {
        try{
            return stepConnectionDetailsBean.get(flowId);
        }catch (Exception e){
            LOGGER.warn("Error occurred while getting steps connections details "+e);
        }
        return null;
    }

}
