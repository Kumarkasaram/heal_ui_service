package com.heal.dashboard.service.beans;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessBean extends BaseEntity {

    private String accessDetails;
    private String userIdentifier;
}
