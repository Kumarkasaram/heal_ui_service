package com.heal.dashboard.service.beans;


import lombok.Data;

import java.sql.Timestamp;

@Data
public class AgentBean {
	private int id;
    private String uniqueToken;
    private String name;
    private int agentTypeId;
    private Timestamp createdTime;
    private Timestamp updatedTime;
    private String userDetailsId;
    private int status;
    private String hostAddress;
    private String mode;
    private String description;
    private Integer compInstanceId;

}

