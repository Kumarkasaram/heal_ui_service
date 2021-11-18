package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class UserAttributeBeen {
    private Integer id;
    private String userIdentifier;
    private String contactNumber;
    private String emailAddress;
    private String username;
    private String userDetailsId;
    private String createdTime;
    private String updatedTime;
    private Integer status;
    private Integer isTimezoneMychoice;
    private Integer isNotificationsTimezoneMychoice;
    private Integer mstAccessProfileId;
    private Integer mstRoleId;
}
