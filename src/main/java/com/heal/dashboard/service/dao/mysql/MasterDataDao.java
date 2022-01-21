package com.heal.dashboard.service.dao.mysql;



import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.heal.dashboard.service.beans.CategoryDetailBean;
import com.heal.dashboard.service.beans.ConnectionDetails;
import com.heal.dashboard.service.beans.FileSummaryDetailsBean;
import com.heal.dashboard.service.beans.MasterKPIDetailsBean;
import com.heal.dashboard.service.beans.ViewTypeBean;
import com.heal.dashboard.service.exception.ServerException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class MasterDataDao {

	@Autowired
	JdbcTemplate jdbcTemplate;

	public List<ConnectionDetails> getConnectionDetails(int accountId) throws ServerException {
		try {
			String query = "select id,source_id ,source_ref_object ,destination_id ,destination_ref_object,account_id ,user_details_id  from connection_details where account_id = ?";
			return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(ConnectionDetails.class), accountId);
		} catch (DataAccessException e) {
			log.error("Error in MasterDataDao class   while fetching getConnectionDetails information", e);
		}
		return Collections.emptyList();
	}

	public List<ViewTypeBean> getAllViewTypes() throws ServerException {
		try {
			String query = "select type, typeid, name ,subtypeid from view_types";
			return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(ViewTypeBean.class));
		} catch (DataAccessException e) {
			log.error("Error while fetching view_types information", e);
			throw new ServerException("Error in masterDataDao class while fetching view_types information  : " + e);
		}
	}

	public String getSubTypeNameForSubTypeId(int subTypeId) {
		String query = "select name from view_types where subtypeid=?";

		try {
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(String.class), subTypeId);
		} catch (Exception e) {
			log.warn("ViewTypes Unavailable for subTypeId [{}]", subTypeId);
			return null;
		}
	}

	public ViewTypeBean getTypeInfoFromSubTypeName(String typeName, String subTypeName) {
		String query = "select typeid, subtypeid from view_types where name=" + subTypeName + " and type=" + typeName;

		try {
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(ViewTypeBean.class));
		} catch (Exception e) {
			log.warn("ViewTypes Unavailable for typeName [{}] and subTypeName [{}]", typeName, subTypeName);
			return null;
		}
	}
	public List<FileSummaryDetailsBean> getFileSummaryDetailsByAccount(int  accountId) {
		String query = "select file_processed_id fileProcessedId, `key`, `value`,account_id accountId, is_debug_logs isDebugLogs from file_summary_details where account_id = ?";

		try {
			return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(FileSummaryDetailsBean.class),accountId);
		} catch (Exception e) {
			log.warn("file_processed_id  Unavailable for accountId [{}]", accountId);
			return null;
		}
	}
	
	public CategoryDetailBean getCategoryDetailsForCategory(String  identifier) {
		String query = "select id categoryId, name name, identifier identifier from mst_category_details where identifier = ?";

		try {
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(CategoryDetailBean.class),identifier);
		} catch (Exception e) {
			log.warn("file_processed_id  Unavailable for accountId [{}]", identifier);
			return null;
		}
	}
	public ViewTypeBean getMstSubTypeForSubTypeId(int subTypeId) {
		String query = "select type, typeid, name ,subtypeid from view_types where subtypeid=?";

		try {
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(ViewTypeBean.class), subTypeId);
		} catch (Exception e) {
			log.warn("ViewTypes Unavailable for subTypeId [{}]", subTypeId);
			return null;
		}
	}
	
	public MasterKPIDetailsBean getMasterKPIDetailsData(int kpiId,int defaultAccId,int accountId) {
		String query = "select id,name,description,data_type  dataType,is_custom  isCustom,status,kpi_type_id  kpiTypeId,measure_units  measureUnits," +
	            "cluster_operation  clusterOperation,created_time  createdTime,updated_time  updatedTime,user_details_id  userDetailsId," +
	            "account_id  accountId,kpi_group_id  kpiGroupId,identifier  identifier,value_type  valueType from mst_kpi_details " +
	            "where id = ? and (account_id = ? or account_id = ?";

		try {
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(MasterKPIDetailsBean.class), kpiId,defaultAccId,accountId);
		} catch (Exception e) {
			log.warn("MasterKPIDetailsBean Unavailable for kpiId [{}]", kpiId);
			return null;
		}
	}
	


}

