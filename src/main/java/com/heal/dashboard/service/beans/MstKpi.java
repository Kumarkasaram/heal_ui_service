package com.heal.dashboard.service.beans;

import java.util.Objects;

import com.heal.dashboard.service.util.Constants;

import lombok.Data;

@Data
public class MstKpi {
	 private int defaultAccountId = Constants.DEFAULT_ACCOUNT_ID;
	    private int accountId;
	    private int kpiId;

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;
	        MstKpi mastKpi = (MstKpi) o;
	        return accountId == mastKpi.accountId &&
	                kpiId == mastKpi.kpiId;
	    }

	    @Override
	    public int hashCode() {
	        return Objects.hash(accountId, kpiId);
	    }
}
