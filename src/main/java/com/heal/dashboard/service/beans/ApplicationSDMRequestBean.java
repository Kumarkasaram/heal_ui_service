package com.heal.dashboard.service.beans;

import lombok.Builder;
import lombok.Data;

@Data
public class ApplicationSDMRequestBean {
    UserAccessDetails userAccessDetails;
    Account account;
    Controller controller;
    long fromTime;
    long toTime;
}
