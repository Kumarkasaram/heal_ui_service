package com.heal.dashboard.service.dao.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class UserAttributeDao {

	 @Autowired
	    JdbcTemplate jdbcTemplate;

		public int getUserAttributeId(String userIdentifier) {
			try {
				String query = "select id from user_attributes where user_identifier = ? and status = 1";
				return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(Integer.class),
						userIdentifier);
			} catch (DataAccessException e) {
				log.error("Error while fetching user access information for user [{}]. Details: ", userIdentifier, e);
				return 0;
			}

			
		}
}
