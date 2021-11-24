package com.heal.dashboard.service.beans.tpf;

import java.util.Objects;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TFPAnomalyTransactionDetails {
    private int transactionId;
    private String transactionName;
    private double responseTime;
    private long volume;
    private int isAnomaly;
    private long slowVolume;
    private long failVolume;
    private long timeoutVolume;
    private long successVolume;
    private long unknownVolume;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TFPAnomalyTransactionDetails details = (TFPAnomalyTransactionDetails) o;
        return transactionId == details.transactionId &&
                transactionName.equals(details.transactionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, transactionName);
    }

}
