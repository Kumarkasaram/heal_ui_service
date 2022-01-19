package com.heal.dashboard.service.beans;


import java.util.List;
import java.util.UUID;

import com.heal.dashboard.service.util.DateTimeUtil;
import com.heal.dashboard.service.util.HealUICache;
import com.heal.dashboard.service.util.StringUtils;

import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.Data;

@Data
public class Account {
    private Integer accountId;
    private String accountName;
    private int status;
    private String privateKey;
    private String publicKey;
    private List<Tags> tags;
    private long timezoneMilli;
    private String timeZoneString;
    private Long updatedTime;
    private String updatedTimeString;
    private String updatedBy;
    private String dateFormat = "YYYY-MM-DD";
    private String timeFormat = "HH:mm";
    private String identifier;
    private Long timeOffset;
    private String offsetName;
    private String abbreviation;
    private String userDetailsId;

    public void setUpdatedTimeString(String updatedTimeString) {
        this.updatedTimeString = updatedTimeString;
        this.updatedTime = DateTimeUtil.getGMTToEpochTime(updatedTimeString);
    }

    public void setUpdatedBy(String updatedBy) {
        KeyCloakUserDetails keyCloakUserDetails = HealUICache.INSTANCE.getKeycloakUserDetails(updatedBy);
        if(keyCloakUserDetails != null)
            this.updatedBy = keyCloakUserDetails.getUsername();
        else
            this.updatedBy = updatedBy;
    }

    public void validate() throws Exception {
        if (StringUtils.isEmpty(this.accountName)) throw new Exception("Name can not be empty or null");
        if(StringUtils.isEmpty(this.identifier)) this.identifier = UUID.randomUUID().toString();
        if (this.tags != null && !tags.isEmpty()) {
            for (Tags tag : tags) {
                if (StringUtils.isEmpty(tag.getName())) throw new Exception("tag name can not be empty or null");
                if (tag.getValue() != null && tag.getValue().length() < 1) throw new Exception("tag value can not be empty");
                if (StringUtils.isEmpty(tag.getIdentifier())) throw new Exception("tag identifier can not be empty or null");
            }
        }

        if(accountName.trim().length() < 2) {
            throw new Exception("Account name should be minimum 2 characters.");
        } else if(accountName.trim().length() > 32) {
            throw new Exception("Account name should not exceed more than 32 characters.");
        }

        if(this.identifier.trim().length() < 2) {
            throw new Exception("Account identifier should be minimum 2 characters.");
        } else if(this.identifier.trim().length() > 64) {
            throw new Exception("Account identifier should not exceed more than 64 characters.");
        }

        if(!this.accountName.matches("^[a-zA-Z0-9-_'.']*$")) {
            throw new Exception("Account name should be A-Z,a-z,0-9,underscore,period and hypen characters.");
        }

        if(!this.identifier.matches("^[a-zA-Z0-9-_'.']*$")) {
            throw new Exception("Account identifier should be A-Z,a-z,0-9,underscore,period and hypen characters.");
        }
    }
}
