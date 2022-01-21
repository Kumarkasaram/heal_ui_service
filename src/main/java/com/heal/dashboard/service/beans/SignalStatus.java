package com.heal.dashboard.service.beans;

public enum SignalStatus {
	    OPEN("OPEN","Open"),
	    CLOSED("CLOSED","Close"),
	    UPGRADED("UPGRADED","Upgraded");

	    private String type;
	    private String returnType;

	    SignalStatus(String type, String returnType) {
	        this.type = type;
	        this.returnType = returnType;
	    }

	    public String getType() {
	        return type;
	    }

	    public String getReturnType(){
	        return returnType;
	    }

	    @Override
	    public String toString() {
	        return type;
	    }
}
