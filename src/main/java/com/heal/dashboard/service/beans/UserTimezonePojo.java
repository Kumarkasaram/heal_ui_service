package com.heal.dashboard.service.beans;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class UserTimezonePojo {
	
		@JsonProperty(required =true)
		@Min(0)
		@Max(1)
		@NotNull
	 	private int isTimezoneMychoice;
		
		@JsonProperty(required =true)
		@Min(0)
		@Max(1)
		@NotNull
	    private int isNotificationsTimezoneMychoice;
	
		@JsonProperty(required =true)
		@Min(0)
		@NotNull
	    private int timezoneId;

}
