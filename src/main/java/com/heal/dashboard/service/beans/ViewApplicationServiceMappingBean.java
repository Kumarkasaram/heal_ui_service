package com.heal.dashboard.service.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewApplicationServiceMappingBean {
    private int applicationId;
    private String applicationName;
    private String applicationIdentifier;
    private int serviceId;
    private String serviceName;
    private String serviceIdentifier;
    private int accountId;
}