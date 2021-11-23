package com.heal.dashboard.service.beans.tpf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TFPServiceTransaction {
    private String serviceName;
    private long volume;
    private String peerService;
    private TransactionDirection direction;
    private String txnId;
}
