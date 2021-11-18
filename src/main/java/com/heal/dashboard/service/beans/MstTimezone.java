package com.heal.dashboard.service.beans;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class MstTimezone extends BaseEntity {

    private String timeZoneId;
    private int timeoffset;
    private String offsetName;
    private String abbreviation;
    private String userDetailsId;
    private int accountId;
    private int status;

}
