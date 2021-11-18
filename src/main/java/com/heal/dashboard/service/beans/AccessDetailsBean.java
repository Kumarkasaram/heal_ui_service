package com.heal.dashboard.service.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessDetailsBean {

    private List<String> accounts;
    private Map<String, Application> accountMapping;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static
    class Application {
        private List<String> applications;
    }
}
