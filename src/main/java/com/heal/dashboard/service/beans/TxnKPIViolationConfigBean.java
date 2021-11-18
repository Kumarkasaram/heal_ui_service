package com.heal.dashboard.service.beans;


import com.heal.dashboard.service.enums.ViolationType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TxnKPIViolationConfigBean {
		private ViolationType txnKpiType;
	    private String responseTimeType;
	    private List<KpiViolationConfigBean> kpiViolationConfig = new ArrayList<>();
}
