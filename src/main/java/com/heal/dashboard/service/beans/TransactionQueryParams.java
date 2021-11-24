package com.heal.dashboard.service.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionQueryParams {
	  private String accountId;
	    private String groupId;
	    private String applicationId;
	    private String serviceId;
	    private String txnId;
	    private int aggLevel;
	    private String txnKpiType;
	    private String responseType;
	    private long fromTime;
	    private long toTime;
	    private String agentId;
	    private int transactionId;
}
