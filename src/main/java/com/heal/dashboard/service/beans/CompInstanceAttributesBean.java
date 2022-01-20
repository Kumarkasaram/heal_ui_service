package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class CompInstanceAttributesBean {
    private int id;
    private String attributeValue;
    private int compInstanceId;
    private int mstComponentAttributeMappingId;
    private String createdTime;
    private String updatedTime;;
    private String userDetailsId;
    private int mstCommonAttributesId;
    private String attributeName;
    private int isMandatory;
    private String displayName;
    private int isUiVisible;
}
