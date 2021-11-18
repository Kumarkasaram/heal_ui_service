package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class UserDetailsBean {
    private Integer id;
    private String userIdentifier;
    private String userName;
    private String role;
    private String userProfile;
    private int status;
    private String createdOn;
    private String createdBy;
}
