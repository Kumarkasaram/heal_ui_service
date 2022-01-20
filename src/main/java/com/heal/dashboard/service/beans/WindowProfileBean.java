package com.heal.dashboard.service.beans;

import lombok.Data;

@Data
public class WindowProfileBean {
    private int profileId;
    private String profileName;
    private int dayOptionId;
    private String dayOptionName;
    private int status;
    private int accountId;
    private String day;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;
    private int isBusinessHour;
}
