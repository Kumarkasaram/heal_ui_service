package com.heal.dashboard.service.dao.mysql;


import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.heal.dashboard.service.beans.AgentBean;
import com.heal.dashboard.service.exception.ServerException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class AgentDao {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public List<Integer> getJimAgentIds(int agentTypeTId) {
        try {
            String query = "select id from agent where agent_type_id=?";
            return jdbcTemplate.query(query, (rs, rowNum) -> rs.getInt("id"), agentTypeTId);
        } catch (Exception e) {
            log.error("Error while fetching JIM agents. Details: ", e);
        }

		return Collections.emptyList();
    }
    public  List<AgentBean> getAgentList() throws ServerException {
  		try {
  			String query = "select id,unique_token ,name,agent_type_id ,created_time ,updated_time ,user_details_id ,status,host_address ,mode,description from agent";
  			return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(AgentBean.class));
  		} catch (DataAccessException e) {
  			log.error("Error while fetching  Detail getAgentList()", e);
  			throw new ServerException("Error while fetching  Detail getAgentList()");
  		}
  	}
}

