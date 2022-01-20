package com.heal.dashboard.service.beans;

import lombok.Data;

import java.util.List;

@Data
public class AllAccountDetails {
    List<TagMapping> tagMappingDetailsList;
    List<ConnectionDetails> connectionDetailsList;
    List<CompInstClusterDetails> compInstanceDetailsList;
    List<ComponentKpis> componentKpisList;
    List<TagDetails>  tagDetailsBeanList;
    List<AllKpiList> allKpiLists;
    List<ClusterInstanceMapping> clusterInstanceMappingList;
    List<TxnAndGroupBean> txnAndGroupBeanList;
    List<WindowProfileBean> windowProfileBeanList;
}
