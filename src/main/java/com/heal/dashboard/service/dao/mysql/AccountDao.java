package com.heal.dashboard.service.dao.mysql;



import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.UserAccessBean;
import com.heal.dashboard.service.exception.ServerException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class AccountDao {

    @Autowired
    JdbcTemplate jdbcTemplate;

	public UserAccessBean fetchUserAccessDetailsUsingIdentifier(String userIdentifier) {
		try {
			String query = "select a.access_details, a.user_identifier from user_access_details a where user_identifier=?";
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(UserAccessBean.class),
					userIdentifier);
		} catch (DataAccessException e) {
			log.error("Error while fetching user access information for user [{}]. Details: ", userIdentifier, e);
		}

		return null;
	}

	public List<AccountBean> getAccountDetails(String timezoneKey, String accountTableName) {
		String query = "select a.id, a.status, mt.timeoffset, mt.timeoffset, a.name, a.identifier, mt.time_zone_id, a.updated_time, " +
				"a.user_details_id, mt.abbreviation, mt.offset_name FROM account a join tag_mapping as tm on tm.object_id = a.id join tag_details as td on tm.tag_id = td.id " +
				"join mst_timezone as mt on mt.id = tm.tag_key where tm.object_ref_table = ? and td.name = ? and a.status = 1";
		return jdbcTemplate.query(query, new Object[]{accountTableName, timezoneKey}, BeanPropertyRowMapper.newInstance(AccountBean.class));

	}

    public AccountBean getAccountDetailsForIdentifier(String accountIdentifier) {
        String query = "select a.id as accountId, a.status, a.name, a.identifier, a.user_details_id as userDetailsId " +
				"FROM account a where a.identifier=?";

		try {
			return jdbcTemplate.queryForObject(query, BeanPropertyRowMapper.newInstance(AccountBean.class), accountIdentifier);
		} catch (Exception e) {
			log.error("Exception encountered while fetching accounts information. Details: ", e);
		}

		return null;
    }
   
    public  List<Integer> getTransactionsIdsForAccount(int accountId) throws ServerException {
  		try {
  			String query = "select id from transaction where account_id=? and status=1";
  			return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(Integer.class),accountId);
  		} catch (DataAccessException e) {
  			log.error("Error while fetching controller information", e);
  			throw new ServerException("Error while fetching transaction information for accountId : "+accountId);
  		}
  	}
}
