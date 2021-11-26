package com.heal.dashboard.service.dao.mysql;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.heal.dashboard.service.beans.CompInstClusterDetails;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class ComponentInstanceDao {

	 @Autowired
	    JdbcTemplate jdbcTemplate;

	    public List<CompInstClusterDetails> getClusterListForService(int accountId,int serviceId) {
	        try {
	            String query = "select id instanceId,common_version_id commonVersionId,mst_component_id compId,component_name " +
	                    "componentName,mst_component_type_id mstComponentTypeId,component_type_name componentTypeName, " +
	                    "mst_component_version_id compVersionId,component_version_name componentVersionName,name instanceName," +
	                    "host_id hostId,status, host_name hostName,is_cluster isCluster,identifier,host_address hostAddress," +
	                    "parent_instance_id parentInstanceId from view_component_instance where account_id = ? and " +
	                    "id in(select object_id from tag_mapping where tag_id=(select id from tag_details where name='Controller') " +
	                    "and object_ref_table='comp_instance' and tag_key=?) and status = 1 and is_cluster=1";
	  			return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CompInstClusterDetails.class),accountId,serviceId);
	        } catch (Exception e) {
	            log.error("Error while fetching JIM agents. Details: ", e);
				return Collections.emptyList();
	        }
	    }
	    
	    public List<CompInstClusterDetails> getClustersForService(int serviceId) {
	        try {
	            String query = "select id instanceId, name instanceName, identifier, host_cluster_id hostId, mst_component_type_id mstComponentTypeId, 1 isCluster" +
	    	            " from view_cluster_services where service_id= ? ";
	  			return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CompInstClusterDetails.class),serviceId);
	        } catch (Exception e) {
	            log.error("Error while fetching JIM agents. Details: ", e);
				return Collections.emptyList();
	        }
	    }
	 
	    public List<CompInstClusterDetails> getinstancesForCluster(int clusterId) {
	        try {
	            String query = "select vci.id instanceId,common_version_id commonVersionId,mst_component_id compId," +
	    	            "component_name componentName,mst_component_type_id mstComponentTypeId," +
	    	            "component_type_name componentTypeName, mst_component_version_id compVersionId," +
	    	            "component_version_name componentVersionName,name instanceName, host_id hostId,status, " +
	    	            "host_name hostName,is_cluster isCluster,identifier,host_address hostAddress," +
	    	            "parent_instance_id parentInstanceId from view_component_instance vci, component_cluster_mapping ccm " +
	    	            "where vci.status = 1 and vci.id = ccm.comp_instance_id and ccm.cluster_id = ?";
	  			return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(CompInstClusterDetails.class),clusterId);
	        } catch (Exception e) {
	            log.error("Error while fetching getinstancesForCluster  Details: ", e);
				return Collections.emptyList();
	        }
	    }
	 

}
