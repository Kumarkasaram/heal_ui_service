package com.heal.dashboard.service.beans;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class CategoryDetailBean {
    private int categoryId;
    private String name;
    private String identifier;
    private int kpiTypeId;
    private int isInformative;
    private int isWorkload;
}

