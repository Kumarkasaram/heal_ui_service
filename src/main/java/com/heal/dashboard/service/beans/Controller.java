package com.heal.dashboard.service.beans;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Controller {
    private String appId;
    private String name;
    private int controllerTypeId;
    private long timeOffset;
    private List<ServiceConfigBean> serviceDetails = new ArrayList<>();
	private List<TxnKPIViolationConfigBean> txnViolationConfig = new ArrayList<>();
    private String identifier;
    private boolean monitoringEnabled;
    private int accountId;
    private int status;
}

