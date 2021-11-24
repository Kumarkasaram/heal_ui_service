package com.heal.dashboard.service.beans.util;

import lombok.Data;

@Data
public class RollupTimeMetaData {
	 private int aggLevel;
	    private long from = 0L;
	    private long to = 0L;
}
