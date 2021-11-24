package com.heal.dashboard.service.beans.tpf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TFPServiceDetails {
    private String serviceName;
    private int serviceId;
    private String applicationName;
    private boolean userAccess;
    private long volume;
    private int transactionCount;
    private int clusterId;
    private int isJIMEnabled;
    private String transactionId;
    private int isFailed;
    private int isSlow;
}
