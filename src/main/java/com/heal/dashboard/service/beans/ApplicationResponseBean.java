package com.heal.dashboard.service.beans;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ApplicationResponseBean {
	private int accountId;
	private List<Controller> accessibleApplications;
	
}
