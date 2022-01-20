package com.heal.dashboard.service.beans;


import lombok.Builder;
import lombok.Data;

@Data
public class CategoryEvents
{

    private int categoryId;
    private String categoryName;
    private long eventCount;
    private boolean forensicEnabled;
    private boolean isInfo;
}
