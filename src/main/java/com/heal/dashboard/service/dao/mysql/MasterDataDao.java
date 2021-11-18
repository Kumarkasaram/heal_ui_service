package com.heal.dashboard.service.dao.mysql;



import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.heal.dashboard.service.beans.ConnectionDetails;
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
			log.error("Error while fetching tag_details information", e);
			throw new ServerException("Error in getConnectionDetails() method  while fetching connection_details information for accountId : " + accountId);
		}
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

}

