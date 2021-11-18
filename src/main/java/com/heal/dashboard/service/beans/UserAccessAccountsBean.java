package com.heal.dashboard.service.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserAccessAccountsBean {

    private AccountMappingBean accountMappingBean;
    private List<AccountBean> accounts;
}
