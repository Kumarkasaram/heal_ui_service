package com.heal.dashboard.service.beans;

import java.util.Objects;

import lombok.Data;

@Data
public class CompInstClusterDetails {
	 private int instanceId;
	    private int status;
	    private int commonVersionId;
	    private int compId;
	    private int mstComponentTypeId;
	    private int compVersionId;
	    private String instanceName;
	    private int hostId;
	    private String hostName;
	    private int isCluster;
	    private String identifier;
	    private String componentName;
	    private String componentTypeName;
	    private String componentVersionName;
	    private String hostAddress;
	    private int parentInstanceId;

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;
	        CompInstClusterDetails that = (CompInstClusterDetails) o;
	        return instanceId == that.instanceId &&
	                status == that.status &&
	                commonVersionId == that.commonVersionId &&
	                compId == that.compId &&
	                mstComponentTypeId == that.mstComponentTypeId &&
	                compVersionId == that.compVersionId &&
	                hostId == that.hostId &&
	                isCluster == that.isCluster &&
	                Objects.equals(instanceName, that.instanceName) &&
	                Objects.equals(hostName, that.hostName) &&
	                Objects.equals(identifier, that.identifier) &&
	                Objects.equals(componentName, that.componentName) &&
	                Objects.equals(componentTypeName, that.componentTypeName) &&
	                Objects.equals(componentVersionName, that.componentVersionName) &&
	                Objects.equals(hostAddress, that.hostAddress);
	    }

	    @Override
	    public int hashCode() {
	        return Objects.hash(instanceId, status, commonVersionId, compId, mstComponentTypeId, compVersionId, instanceName, hostId, hostName, isCluster, identifier, componentName, componentTypeName, componentVersionName, hostAddress);
	    }
}
