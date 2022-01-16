package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class FileSummaryDetailsBean {
    private int fileProcessedId;
    private String key;
    private String value;
    private int accountId;
    private int isDebugLogs;
}
