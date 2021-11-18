package com.heal.dashboard.service.businesslogic;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.stereotype.Component;

import com.datastax.driver.core.Row;
import com.heal.dashboard.service.beans.AccountBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MaintainanceWindowsBL {

	
	public boolean getServiceMaintenanceStatus(String serviceId, Timestamp toTime){
        try {
            List<Row> completedMaintenanceWindows = null;
           // completedMaintenanceWindows = getServiceMaintenanceWindowList(account.getIdentifier(), serviceId);
            if(!completedMaintenanceWindows.isEmpty())
                for(Row maintenanceWindow : completedMaintenanceWindows){
                    Timestamp startTimeMW = new Timestamp(maintenanceWindow.getLong("start_time"));
                    Timestamp endTimeMW = new Timestamp(maintenanceWindow.getLong("end_time"));
                    if(startTimeMW.before(toTime) && endTimeMW.after(toTime)){
                        return true;
                    }
                }
        } catch (Exception e) {
            log.info("Error Occurred while checking Maintenance For Service :{}", serviceId);
            return false;
        }
        return false;
    }
}

