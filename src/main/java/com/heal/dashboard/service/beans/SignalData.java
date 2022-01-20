package com.heal.dashboard.service.beans;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class SignalData {

	  private String id;
	    private String type;
	    private String currentStatus;
	    private String severity;
	    private String description = "under construction...";
	    private Long startTimeMilli;
	    private Long updatedTimeMilli;
	    private Set<SignalData.Application> applications;
	    private Set<String> affectedServices;
	    private String entryServiceId;
	    private Set<String> rootCauseServiceSet;
	    private long eventCount;
	    private Long lastEventTime;
	    private String metricCategory;
	    private Set<SignalData.Services> services;

	    public void addApplications(List<Controller> apps) {
	        if (apps != null) {
	            if (this.applications == null) {
	                this.applications = new HashSet<>();
	            }
	            apps.forEach(app -> this.applications.add(new SignalData.Application(Integer.valueOf(app.getAppId()),
	                    app.getName(), app.getIdentifier())));
	        }
	    }

	    public void addServices(Controller app){
	        if(app != null){
	            if(this.services == null){
	                this.services = new HashSet<>();
	            }
	            this.services.add(new SignalData.Services(Integer.valueOf(app.getAppId()), app.getIdentifier()));
	        }
	    }


	    @AllArgsConstructor
	    @EqualsAndHashCode
	    @Data
	    static class Application{
	        private int id;
	        private String name;
	        private String identifier;
	    }

	    @AllArgsConstructor
	    @EqualsAndHashCode
	    @Data
	    static class Services{
	        private int id;
	        private String name;
	    }
}
