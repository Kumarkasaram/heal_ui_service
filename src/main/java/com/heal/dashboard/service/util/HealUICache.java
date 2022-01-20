package com.heal.dashboard.service.util;

import com.appnomic.appsone.api.beans.*;
import com.appnomic.appsone.api.cache.keys.*;
import com.appnomic.appsone.api.common.Constants;
import com.appnomic.appsone.api.pojo.*;
import com.appnomic.appsone.api.pojo.xpt.BizOps.EventDetail;
import com.appnomic.appsone.api.pojo.xpt.BizOps.XptTransactionAttributes;
import com.appnomic.appsone.api.service.KeyCloakAuthService;
import com.appnomic.appsone.api.service.TopologyDetailsService;
import com.appnomic.appsone.api.service.mysql.*;
import com.appnomic.appsone.api.util.ConfProperties;
import com.appnomic.appsone.api.util.KpiThresholdUtility;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Sagala Prasad on 1/12/20
 */
public enum HealUICache {
    INSTANCE;
    private static final Logger log = LoggerFactory.getLogger(HealUICache.class);
    private final Integer maxSize = ConfProperties.getInt(Constants.CACHE_MAXIMUM_SIZE_PROPERTY_NAME,
            Constants.CACHE_MAXIMUM_SIZE_DEFAULT_VALUE);
    private final Integer cacheTimeout = ConfProperties.getInt(Constants.CACHE_TIMEOUT_IN_MINUTES_PROPERTY_NAME,
            Constants.CACHE_TIMEOUT_IN_MINUTES_DEFAULT_VALUE);
    private int requestThreshold = ConfProperties.getInt(Constants.REQUEST_THRESHOLD_PROPERTY,
            Constants.REQUEST_THRESHOLD_PROPERTY_DEFAULT_VALUE);
    private int requests = 0, unauthorizedRequests = 0, accessDeniedRequests = 0, skipValidationRequests = 0, slowRequests = 0;
    private Map<String, Double> slowRequestDetails = new HashMap<>();
    private double maxRespTimeInMillSecs = 0.0;
    private Map<Integer, Integer> statusCodes = new HashMap<>();

    public int getRequestThreshold() {
        return requestThreshold;
    }

    public void setRequestThreshold(int requestThreshold) {
        this.requestThreshold = requestThreshold;
    }

    public int getRequests() {
        return requests;
    }

    public void updateRequests(int count) {
        this.requests += count;
    }

    public int getUnauthorizedRequests() {
        return unauthorizedRequests;
    }

    public void updateUnauthorizedRequests(int count) {
        this.unauthorizedRequests += count;
    }

    public int getAccessDeniedRequests() {
        return accessDeniedRequests;
    }

    public void updateAccessDeniedRequests(int count) {
        this.accessDeniedRequests += count;
    }

    public int getSkipValidationRequests() {
        return skipValidationRequests;
    }

    public void updateSkipValidationRequests(int count) {
        this.skipValidationRequests += count;
    }

    public int getSlowRequests() {
        return slowRequests;
    }

    public void updateSlowRequests(int count) {
        this.slowRequests += count;
    }

    public Map<String, Double> getSlowRequestDetails() {
        return slowRequestDetails;
    }

    public void resetSlowRequestDetails() {
        slowRequestDetails = new HashMap<>();
    }

    public void addSlowRequestDetails(String api, Double timeInMillSecs) {
        slowRequestDetails.put(api, timeInMillSecs);
    }

    public Map<Integer, Integer> getStatusCodes() {
        return statusCodes;
    }

    public void resetStatusCodes() {
        statusCodes = new HashMap<>();
    }

    public void addStatusCodes(Integer statusCode,Integer counter) {
        statusCodes.put(statusCode, statusCodes.getOrDefault(statusCode,0)+counter);
    }

    public double getMaxRespTimeInMillSecs() {
        return maxRespTimeInMillSecs;
    }

    public void updateResponseTime(String url, double respTimeInMillSecs) {
        if( respTimeInMillSecs > this.maxRespTimeInMillSecs) {
            this.maxRespTimeInMillSecs = respTimeInMillSecs;
        }
        if(respTimeInMillSecs >= requestThreshold * 1000) {
            this.updateSlowRequests(1);
            this.addSlowRequestDetails(url, maxRespTimeInMillSecs);
        }
    }

    /**
     * key: userIdentifier
     * value: KeyCloakUserDetails object with user details
     */
    private LoadingCache<String,KeyCloakUserDetails> keycloakUserData = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, KeyCloakUserDetails>() {
            @Override
            public KeyCloakUserDetails load(String userIdentifier) {
                return KeyCloakAuthService.getKeycloakUserDataFromId(userIdentifier);
            }
        });

    private LoadingCache<Integer,KpiCategoryDetailBean> kpiCategoryDetailCache = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, KpiCategoryDetailBean>() {
            @Override
            public KpiCategoryDetailBean load(Integer kpiId) {
                return MasterDataService.getKpiCategoryDetails(kpiId);
            }
        });

    private LoadingCache<CategoryKey, CategoryDetailBean> CategoryDetailCache = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<CategoryKey, CategoryDetailBean>() {
            @Override
            public CategoryDetailBean load(CategoryKey CategoryKey) {
                return MasterDataService.getCategoryDetails(CategoryKey.getAccountId(),
                        CategoryKey.getCategoryId());
            }
        });

    private LoadingCache<Integer, Integer> categoryToForensicIdMapppingCache = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, Integer>() {
            @Override
            public Integer load(Integer categoryId) {
                return MasterDataService.getForenssicIdForCategoryId(categoryId);
            }
        });

    private LoadingCache<Integer, List<EventDetail>> eventDetailsForTransaction = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<EventDetail>>() {
            @Override
            public List<EventDetail> load(Integer txnId) {
                return TransactionDataService.getEventDetailsForTransaction(txnId);
            }
        });

    private LoadingCache<Integer, List<XptTransactionAttributes>> attributesForXptTransaction = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<XptTransactionAttributes>>() {
            @Override
            public List<XptTransactionAttributes> load(Integer txnId) {
                return TransactionDataService.getAttributesForXptTransaction(txnId);
            }
        });

    private LoadingCache<Integer, List<String>> compInstanceAgents = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<String>>() {
            @Override
            public List<String> load(Integer compInstanceId) {
                return ComponentAgentDataService.getAgentsOfGivenCompInst(compInstanceId);
            }
        });

    private LoadingCache<String, List<Integer>> clusterCompInstanceIds = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, List<Integer>>() {
            @Override
            public List<Integer> load(String clusterIdAccountId) {
                String[] ids = clusterIdAccountId.split(Constants.SEPARATOR);
                return CompInstanceDataService.getListOfCompInstanceIdsForACluster(Integer.parseInt(ids[0]), Integer.parseInt(ids[1]));
            }
        });

    private LoadingCache<KpiThresholdKey, KpiThresholdUtility> componentInstanceKpiThresholdInfo = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<KpiThresholdKey, KpiThresholdUtility>() {
            @Override
            public KpiThresholdUtility load(KpiThresholdKey kpiThresholdKey) {
                return new KpiThresholdUtility(kpiThresholdKey);
            }
        });


    private LoadingCache<Integer, List<TagMappingDetails>> accountTagList = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<TagMappingDetails>>() {

            @Override
            public List<TagMappingDetails> load(Integer accountId) {
                return MasterDataService.getTagMappingDetails(accountId);
            }
        });

    private LoadingCache<Integer, List<ConnectionDetails>> accountConnectionList = CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<ConnectionDetails>>() {

                @Override
                public List<ConnectionDetails> load(Integer accountId){
                    return MasterDataService.getConnectionDetails(accountId);
                }
            });

    private LoadingCache<Integer, List<TransactionGroupDetailBean>> accountTransactionTagDetailsList = CacheBuilder
            .newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<TransactionGroupDetailBean>>() {

                @Override
                public List<TransactionGroupDetailBean> load(Integer accountId){
                    return TransactionDataService.getGroupTagDetailsForAccount(accountId);
                }
            });

    private LoadingCache<Integer, List<Integer>> jimEnabledServiceList = CacheBuilder
            .newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<Integer>>() {

                @Override
                public List<Integer> load(Integer accountId){
                    return TopologyDetailsService.getJIMEnabledServiceIdForAccount(accountId);
                }
            });

    private LoadingCache<AccountServiceKey, List<TxnAndGroupBean>> transactionListForService = CacheBuilder
            .newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<AccountServiceKey, List<TxnAndGroupBean>>() {

                @Override
                public List<TxnAndGroupBean> load(AccountServiceKey key){
                    return TransactionDataService.getTxnAndGroupListForService(key.getAccountId(), key.getServiceId());
                }
            });

    private LoadingCache<AccountServiceKey, List<CompInstClusterDetails>> clusterListForService = CacheBuilder
            .newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<AccountServiceKey, List<CompInstClusterDetails>>() {

                @Override
                public List<CompInstClusterDetails> load(AccountServiceKey key){
                    return CompInstanceDataService.getClusterListOfService(key.getAccountId(), key.getServiceId());
                }
            });

    private LoadingCache<String, List<CategoryDetailBean>> categoryListForTransactionKpis = CacheBuilder
            .newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<CategoryDetailBean>>() {

                @Override
                public List<CategoryDetailBean> load(String key){
                    return MasterDataService.getCategoryForTransactionKpis();
                }
            });
    private LoadingCache<String, List<CategoryDetailBean>> categoryListForWorkloadKpis = CacheBuilder
            .newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<CategoryDetailBean>>() {

                @Override
                public List<CategoryDetailBean> load(String key){
                    return MasterDataService.getCategoryForWorkloadKpis();
                }
            });
    private LoadingCache<Integer, List<CategoryDetailBean>> categoryListForCompInstance = CacheBuilder
            .newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
            .build(new CacheLoader<Integer, List<CategoryDetailBean>>() {

                @Override
                public List<CategoryDetailBean> load(Integer compInstanceId){
                    return MasterDataService.getCategoriesForInstance(compInstanceId);
                }
            });


    private LoadingCache<Integer, CompInstClusterDetails> instanceDetails = CacheBuilder
        .newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, CompInstClusterDetails>() {

            @Override
            public CompInstClusterDetails load(Integer key) {
                return MasterDataService.getCompInstance(key);
            }
        });

    /**
     * key: 'allTimezones'
     * value: list of all time zones available in DB
     */
    private LoadingCache<String,List<TimezoneDetail>> allTimezones = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, List<TimezoneDetail>>() {
            @Override
            public List<TimezoneDetail> load(String key) {
                return new TimezoneDataService().getAllTimezones();
            }
        });

    /**
     * key: account id
     * value: list of group kpi details
     */
    private LoadingCache<Integer, List<MasterKpiGroupBean>> masterKpiGroupList = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<MasterKpiGroupBean>>() {
            @Override
            public List<MasterKpiGroupBean> load(Integer accountId) {
                return MasterDataService.getMasterKpiGroupDetails(Constants.DEFAULT_ACCOUNT_ID, accountId);
            }
        });

    private LoadingCache<Integer,List<TransactionAttributes>> transactionAttributes = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterAccess(cacheTimeout,TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<TransactionAttributes>>() {
            @Override
            public List<TransactionAttributes> load(Integer transactionId) {
                return MasterDataService.getTransactionAttributes(transactionId);
            }
        });

    private LoadingCache<Integer,List<ApplicationThresholdDetailsBean>> applicationThresholdDetails = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterAccess(cacheTimeout,TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<ApplicationThresholdDetailsBean>>() {
            @Override
            public List<ApplicationThresholdDetailsBean> load(Integer accountId) {
                return MasterDataService.getApplicationThresholdDetailsList(accountId);
            }
        });

    private LoadingCache<String, List<ApplicationTagDetailsBean>> applicationTagDetails = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterAccess(cacheTimeout,TimeUnit.MINUTES)
        .build(new CacheLoader<String, List<ApplicationTagDetailsBean>>() {
            @Override
            public List<ApplicationTagDetailsBean> load(String str) {
                String[] params = str.split(Constants.SEPARATOR);
                Integer accountId = Integer.valueOf(params[0].trim());
                String refTable = params[1].trim();
                Integer applicationId = Integer.valueOf(params[2].trim());
                return MasterDataService.getApplicationTagDetails(accountId, refTable, applicationId);
            }
        });

    private LoadingCache<Integer, List<CompInstanceKpiDetailsBean>> compInstanceNonGroupKpiList = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<CompInstanceKpiDetailsBean>>() {
            @Override
            public List<CompInstanceKpiDetailsBean> load(Integer compInstanceId) {
                return MasterDataService.getNonGroupKpiListForCompInst(compInstanceId);
            }
        });

    private LoadingCache<Integer, List<CompInstanceKpiGroupDetailsBean>> compInstanceGroupKpiList = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<CompInstanceKpiGroupDetailsBean>>() {
            @Override
            public List<CompInstanceKpiGroupDetailsBean> load(Integer compInstId) {
                return MasterDataService.getGroupKpiListForCompInst(compInstId);
            }
        });

    private LoadingCache<Host, ComponentInstanceBean> hosts = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Host, ComponentInstanceBean>() {
            @Override
            public ComponentInstanceBean load(Host key) {
                return MasterDataService.getHostsData(key.getHostAddress(), key.getAccountId());
            }
        });

    private LoadingCache<String, ComponentInstanceBean> compInstancesForAccountCompInstName = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, ComponentInstanceBean>() {
            @Override
            public ComponentInstanceBean load(String key) {
                String[] strings = key.split(Constants.SEPARATOR);
                return MasterDataService.getCompInstForAccountComInstName(strings[0], Integer.valueOf(strings[1]));
            }
        });

    /**
     * Key : account id-component instance name
     * Value : Component instance object
     */
    private LoadingCache<String, ComponentInstanceBean> compInstancesForAccountCompInstId = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, ComponentInstanceBean>() {
            @Override
            public ComponentInstanceBean load(String key) {
                String[] strings = key.split(Constants.SEPARATOR);
                return MasterDataService.getCompInstForAccountComInstId(Integer.valueOf(strings[0]), Integer.valueOf(strings[1]));
            }
        });

    private LoadingCache<String, List<AllKpiList>> viewAllKpis = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterAccess(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, List<AllKpiList>>() {
            @Override
            public List<AllKpiList> load(String key) {
                return MasterDataService.getAllKpisList();
            }
        });

    private LoadingCache<Integer, AllAccountDetails> accountDetailsCache = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, AllAccountDetails>() {

            @Override
            public AllAccountDetails load(Integer accountId) {
                return loadAccountDetails(accountId);
            }
        });

    /**
     * key : accountId
     * value : list of all applications
     */
    private LoadingCache<Integer, List<Controller>> controllerDetails = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<Controller>>() {
            @Override
            public List<Controller> load(Integer accountId) {
                return ControllerDataService.getControllerList(accountId);
            }
        });

    /**
     * key: "agentId"
     * value: ComponentAgentBean
     */
    private LoadingCache<Integer, ComponentAgentBean> componentAgentBean = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, ComponentAgentBean>() {
            @Override
            public ComponentAgentBean load(Integer key) {
                return ComponentAgentDataService.getComponentAgent(key);
            }
        });

    /**
     * key: "DataCommunication"
     * value: DataCommunicationDetailsBean
     */
    private LoadingCache<DataCommunication, DataCommunicationDetailsBean> dataCommunicationDetails = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<DataCommunication, DataCommunicationDetailsBean>() {
            @Override
            public DataCommunicationDetailsBean load(DataCommunication key) {
                return DataCommunicationDataService.getDataCommunicationDetails(key.getName());
            }
        });

    /**
     * Key : agent uid
     * value : Master  KPI details object
     */
    private LoadingCache<String, TagDetailsBean> tagDetails = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, TagDetailsBean>() {
            @Override
            public TagDetailsBean load(String key) {
                return MasterDataService.getTagDetails(key);
            }
        });

    /**
     * Key : agent name
     * value : Agent bean
     */
    private LoadingCache<String, AgentBean> agentListNameAsKey = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, AgentBean>() {
            @Override
            public AgentBean load(String key) {
                return AgentDataService.getAgentBeanDataForName(key);
            }
        });

    /**
     * Key : agent uid
     * value : Master  KPI details object
     */
    private LoadingCache<String, AgentBean> agentBeans = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, AgentBean>() {
            @Override
            public AgentBean load(String key) {
                return AgentDataService.getAgentBeanData(key);
            }
        });

    /**
     * key : agentDetails
     * value : all agent details
     */
    private LoadingCache<String, List<AgentBean>> agentDetails = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, List<AgentBean>>() {
            @Override
            public List<AgentBean> load(String agentDetails) {
                return AgentDataService.getAgentList();
            }
        });

    /**
     * Key : MstKpi
     * value : Master  KPI details object
     */
    private LoadingCache<MstKpi, MasterKPIDetailsBean> masterKPIDetailsBean = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<MstKpi, MasterKPIDetailsBean>() {
            @Override
            public MasterKPIDetailsBean load(MstKpi key) {
            return MasterDataService.getMasterKPIDetailsData(key.getKpiId(), key.getDefaultAccountId(), key.getAccountId());
            }
        });

    /**
     * Key : we are creating key using these fields-mst_kpi_details_id,mst_component_version_id,mst_component_id,mst_component_type_id
     * value : producer KPIs object
     */
    private LoadingCache<ProducerKpis, ViewProducerKPIsBean> viewProducerKPIsGroup = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<ProducerKpis, ViewProducerKPIsBean>() {
            @Override
            public ViewProducerKPIsBean load(ProducerKpis key) {
            return MasterDataService.getViewProducerGroupKPIsData(key.getMstKpiDetailsId(), key.getMstCompVersionId(),
                    key.getMstCompId(), key.getMstCompTypeId(), key.getDefaultAccountId(), key.getAccountId());
            }
        });

    /**
     * key: kpi_id
     * value: list of component threshold
     */
    private LoadingCache<Integer, List<ComponentKpiThresholdBean>> compKpiThreshold = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<ComponentKpiThresholdBean>>() {
            @Override
            public List<ComponentKpiThresholdBean> load(Integer kpiId) {
                return MasterDataService.getComponentKpiThreshold(kpiId);
            }
        });

    /**
     * Key : we are creating key using these fields-mst_kpi_details_id,mst_component_version_id,mst_component_id,mst_component_type_id
     * value : producer KPIs object
     */
    private LoadingCache<ProducerKpis, ViewProducerKPIsBean> viewProducerKPIsNonGroup = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<ProducerKpis, ViewProducerKPIsBean>() {
            @Override
            public ViewProducerKPIsBean load(ProducerKpis key) {
            return MasterDataService.getViewProducerNonGroupKPIsData(key.getMstKpiDetailsId(), key.getMstCompVersionId(),
                    key.getMstCompId(), key.getMstCompTypeId(), key.getDefaultAccountId(), key.getAccountId());
            }
        });

    /**
     * Key : 'mst_common_version_id'
     * value : list of attributes object
     */
    private LoadingCache<Integer, List<AttributesViewBean>> attributes = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<AttributesViewBean>>() {
            @Override
            public List<AttributesViewBean> load(Integer key) {
                return MasterDataService.getAttributesViewData(key);
            }
        });


    /**
     * Key : 'CommVersionKPIs'
     * value : list of common version KPIs object
     */
    private LoadingCache<CommVersionKPIs, List<ViewCommonVersionKPIsBean>> viewCommonVersionKPIsNonGroup = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<CommVersionKPIs, List<ViewCommonVersionKPIsBean>>() {
            @Override
            public List<ViewCommonVersionKPIsBean> load(CommVersionKPIs key) {
            return MasterDataService.getViewCommonVersionNonGroupKPIsData(key.getMstCommonVersionId(), key.getDefaultAccountId(), key.getAccountId());
            }
        });

    /**
     * Key : 'ALL_TYPES'
     * value : list of type and subtypes
     */
    private LoadingCache<String, List<ViewTypes>> viewAllTypes = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, List<ViewTypes>>() {
            @Override
            public List<ViewTypes> load(String s) {
                return MasterDataService.getAllTypes();
            }
        });


    /**
     * Key : 'masterComponents'
     * value : list of master component objects
     */
    private LoadingCache<String, List<MasterComponentBean>> masterComponents = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<String, List<MasterComponentBean>>() {
            @Override
            public List<MasterComponentBean> load(String s) {
                return MasterDataService.getComponentMasterData(Constants.DEFAULT_ACCOUNT_ID, Integer.valueOf(s));
            }
        });

    /**
     * Key : 'masterComponentsTypes'
     * value : list of master component types objects
     */
    private LoadingCache<Integer, List<MasterComponentTypeBean>> masterComponentTypes = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<Integer, List<MasterComponentTypeBean>>() {
            @Override
            public List<MasterComponentTypeBean> load(Integer accountId) {
                return MasterDataService.getMasterComponentTypeData(Constants.DEFAULT_ACCOUNT_ID, accountId);
            }
        });

    /**
     * Key : 'MstCompVersion'
     * value : list of master component version objects
     */
    private LoadingCache<MstCompVersion, MasterComponentVersionBean> masterComponentsVersion = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<MstCompVersion, MasterComponentVersionBean>() {
            @Override
            public MasterComponentVersionBean load(MstCompVersion key){
                return MasterDataService.getMasterComponentVersionData(key.getMstCompId(), key.getCompVersionName(), key.getDefaultAccountId(), key.getAccountId());
            }
        });

    /**
     * Key : 'CommVersionKPIs'
     * value : list of common version KPIs object
     */
    private LoadingCache<CommVersionKPIs, List<ViewCommonVersionKPIsBean>> viewCommonVersionKPIsGroup = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(cacheTimeout, TimeUnit.MINUTES)
        .build(new CacheLoader<CommVersionKPIs, List<ViewCommonVersionKPIsBean>>() {
            @Override
            public List<ViewCommonVersionKPIsBean> load(CommVersionKPIs key) {
            return MasterDataService.getViewCommonVersionGroupKPIsData(key.getMstCommonVersionId(), key.getDefaultAccountId(), key.getAccountId());
            }
        });
    /**
     * @param accountId
     * @return
     */
    private AllAccountDetails loadAccountDetails(Integer accountId) {

        AllAccountDetails allAccountDetails = new AllAccountDetails();

        long start = System.currentTimeMillis();
        allAccountDetails.setTagMappingDetailsList(MasterDataService.getTagMappingDetails(accountId));
        log.debug("Time taken to fetch tag mapping for account is {} ms.",(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        allAccountDetails.setConnectionDetailsList(MasterDataService.getConnectionDetails(accountId));
        log.debug("Time taken for fetching connection details is {} ms.",(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        allAccountDetails.setCompInstanceDetailsList(MasterDataService.getCompInstanceDetails(accountId));
        log.debug("Time taken to fetch comp instance details is {} ms.",(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        allAccountDetails.setComponentKpisList(MasterDataService.getCompKpiMapping(accountId));
        log.debug("Time taken to fetch comp kpi details is {} ms.",(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        allAccountDetails.setTagDetailsBeanList(MasterDataService.getTagDetailsForAccount(accountId));
        log.debug("Time taken to fetch tag meta details is {} ms.",(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        allAccountDetails.setAllKpiLists(getAllKpi());
        log.debug("Time taken to fetch all kpi details is {} ms.",(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        allAccountDetails.setClusterInstanceMappingList(MasterDataService.getClusterInstanceMapping(accountId));
        log.debug("Time taken to fetch cluster inst mapping details is {} ms.",(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        allAccountDetails.setTxnAndGroupBeanList(TransactionDataService.getTxnAndGroupList(accountId));
        log.debug("Time taken to fetch txn details is {} ms.",(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        allAccountDetails.setWindowProfileBeanList(MasterDataService.getWindowProfileList(accountId));
        log.debug("Time taken to fetch maintenance window details is {} ms.",(System.currentTimeMillis()-start));
        return allAccountDetails;
    }

    public List<ViewTypes> getSubTypeDetails(String mstTypeName) {
        List<ViewTypes> subTypes = new ArrayList<>();
        try {
            /*
              get the master type bean to get id
             */
            subTypes = HealUICache.INSTANCE.viewAllTypes.get(Constants.ALL_TYPES)
                    .stream()
                    .filter(it -> (mstTypeName.trim().equals(it.getTypeName())))
                    .collect(Collectors.toList());


        } catch (Exception e) {
            log.error("Error occurred while getting sub type details from DB. Parameters Typename:{}", mstTypeName, e);
        }
        return subTypes;
    }


    public ViewTypes getTypeDetails(String mstTypeName) {
        ViewTypes typeBean = null;
        try {
            /*
             * get the master type bean to get id
             */
            Optional<ViewTypes> typeBeanOptional = HealUICache.INSTANCE.viewAllTypes.get(Constants.ALL_TYPES)
                    .stream()
                    .filter(it -> (mstTypeName.trim().equals(it.getTypeName())))
                    .findFirst();

            if(typeBeanOptional.isPresent()) {
                typeBean = typeBeanOptional.get();
            }

        } catch (Exception e) {
            log.error("Error occurred while getting type details from DB. Parameters Typename:{}", mstTypeName, e);
        }
        return typeBean;
    }


    public ViewTypes getMstTypeForSubTypeName(String typeName, String subTypeName) {
        ViewTypes subType = null;
        try {
            Optional<ViewTypes> subTypeOptional = HealUICache.INSTANCE.viewAllTypes
                    .get(Constants.ALL_TYPES)
                    .stream()
                    .filter(it -> (typeName.trim().equalsIgnoreCase(it.getTypeName())))
                    .filter(it -> (subTypeName.trim().equalsIgnoreCase(it.getSubTypeName())))
                    .findAny();

            if(subTypeOptional.isPresent())
                subType = subTypeOptional.get();

        } catch (Exception e) {
            log.error("Error occured while fetching subtype data from DB. " +
                    "Parameters Typename:{}, SubTypeName:{}", typeName, subTypeName, e);
        }
        return subType;
    }

    public ViewTypes getMstSubTypeForSubTypeId(int subTypeId)    {
        ViewTypes subType = null;
        try {
            Optional<ViewTypes> subTypeOptional = HealUICache.INSTANCE.viewAllTypes
                    .get(Constants.ALL_TYPES)
                    .stream()
                    .filter(it -> (subTypeId == it.getSubTypeId()))
                    .findAny();

            if(subTypeOptional.isPresent())
                subType = subTypeOptional.get();
        }   catch (Exception e) {
            log.error("Error occurred while fetching master type and sub type details from DB. Parameters SubTypeId:{}", subTypeId, e);
        }
        return subType;
    }


    public List<ViewTypes> getTypeDetailsList(String mstTypeName) {
        try {
            /*
             * get the master type bean to get id
             */
            return HealUICache.INSTANCE.viewAllTypes.get(Constants.ALL_TYPES)
                    .stream()
                    .filter(it -> (mstTypeName.trim().equals(it.getTypeName())))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Exception encountered while fetching view types details. Parameters MstTypeName: {}", mstTypeName, e);
        }
        return Collections.emptyList();
    }

    /**
     * Get master component object using component name
     */
    public MasterComponentBean getMasterComponentUsingName(String mstCompName, String accountId) {
        try {
            List<MasterComponentBean> masterComponentBeanList = masterComponents.get(accountId);
            return masterComponentBeanList.stream().filter(masterComponentBean -> masterComponentBean.getName().equals(mstCompName)).findAny().orElse(null);
        } catch (Exception e) {
            log.error("Exception encountered while fetching component details. Parameters mstCompName: {}, accountId: {}", mstCompName, accountId, e);
        }
        return null;
    }

    /**
     * Get master component type according to name
     */
    public MasterComponentTypeBean getMasterComponentTypeUsingName(String mstCompTypeName, Integer accountId) {
        try {
            List<MasterComponentTypeBean> masterComponentTypeBeanList = masterComponentTypes.get(accountId);
            return masterComponentTypeBeanList.stream().filter(masterComponentTypeBean -> masterComponentTypeBean.getName().equalsIgnoreCase(mstCompTypeName)).findAny().orElse(null);
        } catch (Exception e) {
            log.error("Exception encountered while fetching component type details. Parameters mstCompTypeName: {}, accountId: {}", mstCompTypeName, accountId, e);
        }
        return null;
    }

    public MasterComponentVersionBean getMasterComponentVersionUsingName(MstCompVersion mstCompVersion) {
        try {
            return masterComponentsVersion.get(mstCompVersion);
        } catch (Exception e) {
            log.error("Exception encountered while fetching component version details. Parameters mstCompVersion: {}", mstCompVersion, e);
        }
        return null;
    }

    public List<AttributesViewBean> getAttributesViewBeanList(int mstCommonVersionId) {
        try {
            return attributes.get(mstCommonVersionId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching attributes details. Parameters mstCommonVersionId: {}", mstCommonVersionId, e);
        }
        return Collections.emptyList();
    }

    public List<ViewCommonVersionKPIsBean> getGroupKPIUsingCommonVersionId(CommVersionKPIs commVersionKPIs) {
        try {
            return viewCommonVersionKPIsGroup.get(commVersionKPIs);
        } catch (Exception e) {
            log.error("Exception encountered while fetching group KPI details. Parameters commVersionKPIs: {}", commVersionKPIs, e);
        }
        return Collections.emptyList();
    }

    public List<ViewCommonVersionKPIsBean> getNonGroupKPIUsingCommonVersionId(CommVersionKPIs commVersionKPIs) {
        try {
            return viewCommonVersionKPIsNonGroup.get(commVersionKPIs);
        } catch (Exception e) {
            log.error("Exception encountered while fetching non group KPI details. Parameters commVersionKPIs: {}", commVersionKPIs, e);
        }
        return Collections.emptyList();
    }

    public ViewProducerKPIsBean getViewProducerKPIsNonGroup(ProducerKpis key) {
        try {
            return viewProducerKPIsNonGroup.get(key);
        } catch (Exception e) {
            log.error("Exception encountered while fetching producer details. Parameters key: {}", key, e);
        }
        return null;
    }

    public List<ComponentKpiThresholdBean> getComponentKpiThreshold(int kpiId)   {
        try {
            return compKpiThreshold.get(kpiId);
        }   catch (Exception e) {
            log.error("Exception encountered while fetching component kpi thresholds. Parameters kpiId: {}", kpiId, e);
        }
        return Collections.emptyList();
    }

    public ViewProducerKPIsBean getViewProducerKPIsGroup(ProducerKpis key) {
        try {
            return viewProducerKPIsGroup.get(key);
        } catch (Exception e) {
            log.error("Exception encountered while fetching producer details. Parameters key: {}", key, e);
        }
        return null;
    }

    public MasterKPIDetailsBean getMasterKPIDetailsBean(MstKpi kpiDetailsId) {
        try {
            return masterKPIDetailsBean.get(kpiDetailsId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching kpi details. Parameters kpiDetailsId: {}", kpiDetailsId, e);
        }
        return null;
    }

    public AgentBean getAgentBean(String agentUid) {
        try {
            return agentBeans.get(agentUid);
        } catch (Exception e) {
            log.error("Exception encountered while fetching agent details. Parameters agentUid: {}", agentUid, e);
        }
        return null;
    }

    public AgentBean getAgentBeanForName(String agentName) {
        try {
            return agentListNameAsKey.get(agentName);
        } catch (Exception e) {
            log.error("Exception encountered while fetching agent details. Parameters agentName: {}", agentName, e);
        }
        return null;
    }

    public TagDetailsBean getTagDetails(String tagName) {
        try {
            return tagDetails.get(tagName);
        } catch (Exception e) {
            log.error("Exception encountered while fetching tag details. Parameters tagName: {}", tagName, e);
        }
        return null;
    }


    public DataCommunicationDetailsBean getDataCommunicationDetails(DataCommunication dataCommunication) {
        try {
            return dataCommunicationDetails.get(dataCommunication);
        } catch (Exception e) {
            log.error("Exception encountered while fetching data communication details. Parameters dataCommunication: {}", dataCommunication, e);
        }
        return null;
    }

    public ComponentAgentBean getComponentAgent(int agentId) {
        try {
            return componentAgentBean.get(agentId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching component agent details. Parameters agentId: {}", agentId, e);
        }
        return null;
    }

    public List<AgentBean> getAgentList(String agent) {
        List<AgentBean> agentBeanList = new ArrayList<>();
        try {
            agentBeanList = agentDetails.get(agent);
        } catch (Exception e) {
            log.error("Exception encountered while fetching agent details. Parameters agent: {}", agent, e);
        }
        return agentBeanList;
    }

    public List<Controller> getApplicationList(Integer accountId) {
        List<Controller> controllerList = new ArrayList<>();
        try {
            controllerList = controllerDetails.get(accountId);
        } catch (Exception e) {
            log.error("Error occurred while getting application list from DB. Parameters accountId: {}", accountId, e);
        }
        return controllerList;
    }

    public List<AllKpiList> getAllKpi() {
        try {
            return viewAllKpis.get(Constants.VIEW_ALL_KPI);
        } catch (Exception e) {
            log.error("Error occurred while getting all KPIs from DB.", e);
        }
        return Collections.emptyList();
    }

    public AllAccountDetails getAccountDetailsCache(Integer accountId) {
        try {
            return accountDetailsCache.get(accountId);
        } catch (Exception e) {
            log.error("Error occurred while account details from DB. Parameters accountId: {}", accountId, e);
        }
        return null;
    }

    public ComponentInstanceBean getCompInstUsingAccountIdAndInstId(int instanceId, int accountId) {
        try {
            return compInstancesForAccountCompInstId.get(instanceId + Constants.SEPARATOR + accountId);
        } catch (Exception e) {
            log.error("Error occurred while fetching component instance details. Parameters instanceId: {}, accountId: {}", instanceId, accountId, e);
        }
        return null;
    }

    public ComponentInstanceBean getCompInstUsingAccountIdAndInstName(String name, int accountId) {
        try {
            return compInstancesForAccountCompInstName.get(name + Constants.SEPARATOR + accountId);
        } catch (Exception e) {
            log.error("Error occurred while fetching component instance details. Parameters name: {}, accountId: {}", name, accountId, e);
        }
        return null;
    }

    public ComponentInstanceBean getHost(Host host) {
        try {
            return hosts.get(host);
        } catch (Exception e) {
            log.error("Error occurred while fetching component instance details. Parameters host: {}", host, e);
        }
        return null;
    }

    public List<CompInstanceKpiGroupDetailsBean> getGroupKpiListForInstanceId(int instId) {
        try {
            return compInstanceGroupKpiList.get(instId);
        } catch (Exception e) {
            log.error("Error occurred while fetching group kpis for component instance details. Parameters instId: {}", instId, e);
        }
        return Collections.emptyList();
    }

    public List<CompInstanceKpiDetailsBean> getNonGroupKpiListForInstanceId(int instId) {
        try {
            return compInstanceNonGroupKpiList.get(instId);
        }   catch (Exception e) {
            log.error("Error occurred while fetching non-group kpis for component instance details. Parameters instId: {}", instId, e);
        }
        return Collections.emptyList();
    }

    public List<ApplicationTagDetailsBean> getApplicationTagDetails(Integer accountId, String refTable, Integer applicationId) {
        List<ApplicationTagDetailsBean> applicationTagDetail = new ArrayList<>();
        try{
            String key = accountId + Constants.SEPARATOR + refTable + Constants.SEPARATOR + applicationId;
            applicationTagDetail = applicationTagDetails.get(key);
        }catch (Exception e){
            log.error("Error occurred while getting application timeOffset and service configuration. " +
                    "Parameters accountId:{}, refTable:{}, applicationId: {} ", accountId, refTable, applicationId,e);
        }
        return applicationTagDetail;
    }

    public List<ApplicationThresholdDetailsBean> getApplicationThresholdDetails(Integer accountId) {

        List<ApplicationThresholdDetailsBean> txnViolationConfigBeans = new ArrayList<>();
        try{
            txnViolationConfigBeans = applicationThresholdDetails.get(accountId);
        } catch (Exception e) {
            log.error("Error occurred while getting application threshold data from DB. Parameters accountId:{}", accountId, e);
        }
        return txnViolationConfigBeans;
    }

    public List<TransactionAttributes> getTransactionAttributes(Integer transactionId) {
        List<TransactionAttributes> transactionAttributesList = new ArrayList<>();
        try {
            transactionAttributesList = transactionAttributes.get(transactionId);
        } catch (Exception e) {
            log.error("Error occurred while getting transaction attributes. Parameters transactionId: {}", transactionId, e);
        }
        return transactionAttributesList;
    }

    public void preloadTransactionConfiguration() {
        try{
            //Preload cache initially for better performance of transaction details API
            Integer n = MasterDataService.getMaxTransactionId();
            n = ( n == null ) ? 0 : n;
            for(int i=1; i<=n; i++) {
                transactionAttributes.get(i);
            }

        } catch (Exception e) {
            log.error("Error occurred while pre-loading Transaction Attributes.",e);
        }
    }

    public MasterKpiGroupBean getGroupKpiDetailList(int accountId, int kpiGroupId) {
        MasterKpiGroupBean groupBean = null;
        try {
            Optional<MasterKpiGroupBean> optionalGroupBean = masterKpiGroupList.get(accountId)
                    .stream()
                    .filter(it -> (it.getId() == kpiGroupId))
                    .findAny();

            if(optionalGroupBean.isPresent()) {
                groupBean = optionalGroupBean.get();
            }
        } catch (Exception e) {
            log.error("Error occurred while fetching group kpi details from database. Parameters accountId: {}, kpiGroupId:{}.", accountId, kpiGroupId, e);
        }
        return groupBean;
    }

    public List<TimezoneDetail> getTimezones()   {
        try {
            return allTimezones.get("allTimezones");
        }   catch (Exception e) {
            log.error("Error occurred while fetching timezones from database.", e);
        }
        return Collections.emptyList();
    }


    public CompInstClusterDetails getInstancesDetails(Integer instanceId) {
        try{
            return instanceDetails.get(instanceId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching cluster instance list. Parameters instanceId: {}.", instanceId, e);
        }
        return null;
    }

    public KeyCloakUserDetails getKeycloakUserDetails(String userIdentifier) {
        try {
            return keycloakUserData.get(userIdentifier);
        }   catch (Exception e) {
            log.warn("Exception encountered while fetching user details. Parameters userIdentifier: {}.", userIdentifier, e);
        }
        return null;
    }

    public KpiCategoryDetailBean getKpiCategoryDetails(int kpiId)   {
        try {
            return kpiCategoryDetailCache.get(kpiId);
        }   catch (Exception e) {
            log.warn("Error occurred while fetching kpi category details from DB. Parameters kpiId: {}",kpiId, e);
        }
        return null;
    }

    public CategoryDetailBean getCategoryDetails(int accountId,int categoryId)   {
        try {
            CategoryKey categoryKey=new CategoryKey();
            categoryKey.setAccountId(accountId);
            categoryKey.setCategoryId(categoryId);
            return CategoryDetailCache.get(categoryKey);
        }   catch (Exception e) {
            log.error("Error occurred while fetching kpi category details for accountId:{},categoryId: {}",accountId,categoryId);
        }
        return null;
    }

    public Integer getForensicIdForCategory(int categoryId) {
        try {
            return categoryToForensicIdMapppingCache.get(categoryId);
        }   catch (Exception e) {
            log.warn("Error occurred while fetching forensic id for category.",e);
        }
        return 0;
    }

    public List<EventDetail> getEventForTransaction(int txnId) {
        try {
            return eventDetailsForTransaction.get(txnId);
        } catch (Exception e) {
            log.warn("Error occurred while fetching event details for txnId: {}.",txnId, e);
        }
        return Collections.emptyList();
    }

    public List<XptTransactionAttributes> getAttributesForXptTransaction(int txnId) {
        try {
            return attributesForXptTransaction.get(txnId);
        } catch (Exception e) {
            log.warn("Error occurred while fetching event details for txnId: {}.",txnId, e);
        }
        return Collections.emptyList();
    }

    public List<String> getListOfAgentsForAGivenCompInstId(int compInstId) {
        try {
            return compInstanceAgents.get(compInstId);
        } catch (Exception e) {
            log.warn("Error occurred while fetching list of agents for a given component instance id: {}.",compInstId, e);
        }
        return Collections.emptyList();
    }

    public List<Integer> getListOfCompInstIdForAGivenClusterId(int clusterId, int accountId) {
        try {
            String id = clusterId + Constants.SEPARATOR + accountId;
            return clusterCompInstanceIds.get(id);
        } catch (Exception e) {
            log.warn("Error occurred while fetching list of component ids for a given cluster id: {} and account id : {}",clusterId, accountId, e);
        }
        return Collections.emptyList();
    }

    public ComponentInstanceKpiThresholdBean getThreshold(KpiThresholdKey kpiThresholdKey, long time) {
        ComponentInstanceKpiThresholdBean result = null;
        try {
            KpiThresholdUtility kpiThresholdUtility = componentInstanceKpiThresholdInfo.get(kpiThresholdKey);
            result = kpiThresholdUtility.getThreshold(time);
        } catch (Exception e) {
            log.error("Error occurred while fetching threshold for kpi: {}, for time: {}", kpiThresholdKey, time, e);
        }
        return result;
    }

    public List<TagMappingDetails> getAccountTagList(int accountId) {
        try{
            return accountTagList.get(accountId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching tag list. Parameters Account: {}", accountId, e);
        }
        return Collections.emptyList();
    }

    public List<ConnectionDetails> getAccountConnectionList(int accountId) {
        try{
            return accountConnectionList.get(accountId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching connection list. Account: {}", accountId, e);
        }
        return Collections.emptyList();
    }

    public List<TransactionGroupDetailBean> getAccountTransactionTags(int accountId) {
        try{
            return accountTransactionTagDetailsList.get(accountId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching txn tags list. Account: {}", accountId, e);
        }
        return Collections.emptyList();
    }

    public List<Integer> getJIMEnabledServiceList(int accountId) {
        try{
            return jimEnabledServiceList.get(accountId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching jim enables svc list. Account: {}", accountId, e);
        }
        return Collections.emptyList();
    }

    public List<TxnAndGroupBean> getTransactionListForService(AccountServiceKey key) {
        try{
            return transactionListForService.get(key);
        } catch (Exception e) {
            log.error("Exception encountered while fetching service txn list. Parameters Account: {}, service: {}", key.getAccountId(), key.getServiceId(), e);
        }
        return Collections.emptyList();
    }

    public List<CompInstClusterDetails> getClusterListForService(AccountServiceKey key) {
        try{
            return clusterListForService.get(key);
        } catch (Exception e) {
            log.error("Exception encountered while fetching service comp instance list. Account: {}, service: {}", key.getAccountId(), key.getServiceId(), e);
        }
        return Collections.emptyList();
    }

    public List<CategoryDetailBean> getcategoryForTransactionKpis() {
        try{
            return categoryListForTransactionKpis.get(Constants.ALL);
        } catch (Exception e) {
            log.error("Exception encountered while fetching category for transaction kpi", e);
        }
        return Collections.emptyList();
    }
    public List<CategoryDetailBean> getCategoryForWorkloadKpis() {
        try{
            return categoryListForWorkloadKpis.get(Constants.ALL);
        } catch (Exception e) {
            log.error("Exception encountered while fetching category for transaction kpi", e);
        }
        return Collections.emptyList();
    }

    public List<CategoryDetailBean> getcategoryForInstance(Integer compInstanceId) {
        try{
            return categoryListForCompInstance.get(compInstanceId);
        } catch (Exception e) {
            log.error("Exception encountered while fetching category list for instance: {}.",compInstanceId, e);
        }
        return Collections.emptyList();
    }

}
