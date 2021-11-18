package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class ControllerBean {
    private int id;
    private String name;
    private String identifier;
    private int accountId;
    private String userDetailsId;
    private String createdTime;
    private String updatedTime;
    private int controllerTypeId;
}
