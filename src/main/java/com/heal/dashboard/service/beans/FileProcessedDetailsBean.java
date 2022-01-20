package com.heal.dashboard.service.beans;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class FileProcessedDetailsBean {
    private String fileName;
    private long fileSize;
    private String checksum;
    private String fileLocation;
    private String uploadBy;
    private Timestamp uploadTime;
    private String processedBy;
    private Timestamp startTime;
    private Timestamp endTime;
    private String status;
    private int accountId;
    private int processId;
    private String progress;
}
