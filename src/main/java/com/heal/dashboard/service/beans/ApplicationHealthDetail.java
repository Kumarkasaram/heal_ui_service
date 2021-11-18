package com.heal.dashboard.service.beans;

import com.heal.dashboard.service.dao.mysql.MasterDataDao;
import com.heal.dashboard.service.util.Constants;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

@Data
@NoArgsConstructor
public class ApplicationHealthDetail {

	@Autowired
	private MasterDataDao masterDataDao;

	private int id;
	private String identifier;
	private String name;
	private boolean maintenanceWindowStatus;
	private String dashboardUId;

	private ArrayList<ApplicationHealthStatus> problem = new ArrayList<>();
	private ArrayList<ApplicationHealthStatus> warning = new ArrayList<>();
	private ArrayList<ApplicationHealthStatus> batch = new ArrayList<>();

	public void setApplicationHealthStatus() {
		String severeType = masterDataDao.getSubTypeNameForSubTypeId(Constants.SEVERITY_295);
		String defaultType = masterDataDao.getSubTypeNameForSubTypeId(Constants.SEVERITY_296);

		ApplicationHealthStatus severeTypeProblem = new ApplicationHealthStatus();
		severeTypeProblem.setName(severeType);
		severeTypeProblem.setCount(0);
		severeTypeProblem.setPriority(1);

		ApplicationHealthStatus defaultTypeProblem = new ApplicationHealthStatus();
		defaultTypeProblem.setName(defaultType);
		defaultTypeProblem.setCount(0);
		defaultTypeProblem.setPriority(0);

		problem.add(severeTypeProblem);
		problem.add(defaultTypeProblem);

		ApplicationHealthStatus severeTypeWarning = new ApplicationHealthStatus();
		severeTypeWarning.setName(severeType);
		severeTypeWarning.setCount(0);
		severeTypeWarning.setPriority(1);

		ApplicationHealthStatus defaultTypeWarning = new ApplicationHealthStatus();
		defaultTypeWarning.setName(defaultType);
		defaultTypeWarning.setCount(0);
		defaultTypeWarning.setPriority(0);

		warning.add(severeTypeWarning);
		warning.add(defaultTypeWarning);

		ApplicationHealthStatus batchSevereTypeStr = new ApplicationHealthStatus();
		batchSevereTypeStr.setName(severeType);
		batchSevereTypeStr.setPriority(1);

		batch.add(batchSevereTypeStr);
		batch.add(defaultTypeWarning);
	}
}
