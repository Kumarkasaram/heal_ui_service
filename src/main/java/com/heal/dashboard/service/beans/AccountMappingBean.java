package com.heal.dashboard.service.beans;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class AccountMappingBean {

    private List<String> accounts;
    private Map<String, List<String>> accountMapping;
}