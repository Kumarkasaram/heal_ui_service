package com.heal.dashboard.service.util;


public class Constants {

    public static final String AUTHORIZATION_TOKEN_IS_NULL_OR_EMPTY = "Authorization token is NULL or empty";
    public static final String ACCOUNT_TABLE_NAME_MYSQL_DEFAULT = "account";
    public static final String MESSAGE_INVALID_PARAMETERS = "Invalid input parameter/s provided.";
    public static final String ERROR_INVALID_INPUT_PARAM = "Invalid input parameters provided. Param name:{0}, value:{1}";
    public static final String USER_NOT_EXISTS = "User doesn't exists in HEAL. Identifier: {0}";
    public static final String USER_PREFERENCES_TAG_NAME = "UserPreference";
    public static final String REQUEST_PARAM_FROM_TIME = "fromTime";
    public static final String REQUEST_PARAM_TO_TIME = "toTime";
    public static final String ROLLUP_LEVELS_DEFAULT = "1440,60,30,15,1";
    public static final int DEFAULT_ACCOUNT_ID = 1;
    public static final String USER_ATTRIBUTES_TABLE_NAME_MYSQL = "user_attributes";

    public static final String PROBLEM_LITERAL = "problem";
    public static final String BATCH_JOB_LITERAL = "batch_job";

    public static final String SIGNAL_CLOSE_WINDOW_TIME = "15";

    public static final String LAYER_TAG = "LayerName";
    public static final String TIME_ZONE_TAG = "Timezone";
    public static final String ENTRY_POINT = "EntryPoint";
    public static final String DEFAULT_ENTRY_POINT = "Type";
    public static final String CONTROLLER = "controller";
    public static final String AGENT_TABLE = "agent";
    public static final String SERVICE_TYPE_TAG = "ServiceType";
    public static final String ICON_TITLE_SPLITTER_DEFAULT = "-";
    public static final String CONTROLLER_TAG = "Controller";
    public static final String AGENT_TYPE = "Agent";
    public static final String JIM_AGENT_TYPE = "JIMAgent";
    public static final String KUBERNETES = "Kubernetes";
    
    public static final String CONTROLLER_TYPE_NAME_DEFAULT = "ControllerType";
    public static final String APPLICATION_CONTROLLER_TYPE = "Application";


    public static final int SEVERITY_295 = 295;
    public static final int SEVERITY_296 = 296;

    public static final String TFP_SERVICE = "Service";
    public static final String TFP_PEER_SERVICE = "PeerService";
    public static final String TFP_TXN_ID = "TxnId";
    public static final String TFP_TXN_DIRECTION = "TxnDirection";
    public static final String TFP_TIMESTAMP = "TimestampInGMT";
    public static final String ELASTIC_TIMESTAMP = "@timestamp";
    public static final String TFP_TXN_VOLUME = "Volume";
    public static final String TFP_TXN_STATUS_CODE = "StatusCode";
    public static final String TFP_TXN_AVGRESPTIME = "AvgResponseTime";
    public static final String TFP_INDEX_PREFIX = "tfp-";
    public static final String TFP_TXN_RESPONSES_STATUS_TAG = "ResponseStatusTag";
    public static final String TFP_UNMATCHED_TXN = "unmatched";
    
    
    public static final String CASSANDRA_ALL_IDENTIFIER = "ALL";
    public static final String TRANSACTION_TYPE_DEFAULT = "DC";
    public static final String HOST = "Host";


}

