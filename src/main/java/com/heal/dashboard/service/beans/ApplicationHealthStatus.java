package com.heal.dashboard.service.beans;



import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApplicationHealthStatus {
	private String name;
    private int count;
    private int priority;
    
    
}

