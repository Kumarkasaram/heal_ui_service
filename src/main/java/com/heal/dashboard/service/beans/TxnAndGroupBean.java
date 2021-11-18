package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class TxnAndGroupBean {
    private int txnId;
    private String txnName;
    private int txnStatus;
    private int isAutoEnabled;
    private int isAutoConfigured;
    private String userDetailsId;
    private String transactionTypeName;
    private String patternHashCode;
    private String description;
    private String identifier;
    private int isBusinessTransaction;
    private String tagListString;
    private String serviceId;
}
