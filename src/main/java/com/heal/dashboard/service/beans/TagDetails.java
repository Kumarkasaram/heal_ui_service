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
public class TagDetails extends BaseEntity{

    private String name;
    private String refSelectColumnName;
    private String refWhereColumnName;
    private int tagTypeId;
    private int isPredefined;
    private String refTable;
    private int accountId;
    private String userDetailsId;
}
