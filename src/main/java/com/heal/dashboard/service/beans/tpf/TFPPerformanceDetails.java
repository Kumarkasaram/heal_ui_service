package com.heal.dashboard.service.beans.tpf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TFPPerformanceDetails {
    long success;
    long slow;
    long failed;
    long timedout;
    long unknown;
}
