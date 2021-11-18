package com.heal.dashboard.service.enums;



import java.util.HashMap;
import java.util.Map;

public enum ViolationType {
		SLOW_PERCENTAGE("Slow Percentage"),
	    FAILURE_PERCENTAGE("Fail Percentage"),
	    RESPONSE_TIME("Response Time (ms)"),
	    TOTAL_VOLUME("Total Volume");

	    private String configDBName;
	    private static final Map<String, ViolationType> reverseLookupMap = new HashMap<>();

	    static {
	        for (ViolationType vio : ViolationType.values()) {
	            reverseLookupMap.put(vio.getConfigDBName(), vio);
	        }
	    }

	    ViolationType(String configDBName) {
	        this.configDBName = configDBName;
	    }

	    public String getConfigDBName() {
	        return configDBName;
	    }

	    public static ViolationType getEnum(String strValue) {
	        return reverseLookupMap.get(strValue);
	    }
}
