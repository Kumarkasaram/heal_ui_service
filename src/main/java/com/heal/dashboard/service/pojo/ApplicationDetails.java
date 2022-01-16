package com.heal.dashboard.service.pojo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@Data
@NoArgsConstructor
public class ApplicationDetails {
    private int id;
    private String name;
    private boolean hasTransactionConfigured = false;
    private String identifier;
    private String dashboardUId;

}
