package com.heal.dashboard.service.pojo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ApplicationDetails {
    private int id;
    private String name;
    private boolean hasTransactionConfigured = false;
    private String identifier;
}
