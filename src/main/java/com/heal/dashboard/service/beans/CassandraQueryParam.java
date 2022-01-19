package com.heal.dashboard.service.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CassandraQueryParam {
    private String accountIdentifier;
    private String serviceIdentifier;
    private String instanceIdentifier;
    private String categoryIdentifier;
    private String kpiId;
    private String kpiAttributeId;
    private long fromTime;
    private long toTime;
}
