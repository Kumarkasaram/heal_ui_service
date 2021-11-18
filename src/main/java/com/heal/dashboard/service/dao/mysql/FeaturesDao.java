package com.heal.dashboard.service.dao.mysql;



import java.util.List;

import com.heal.dashboard.service.exception.UiServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.heal.dashboard.service.beans.DateComponentBean;
import com.heal.dashboard.service.beans.MasterFeaturesBean;
import com.heal.dashboard.service.exception.ServerException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class FeaturesDao {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public List<MasterFeaturesBean> getMasterFeatures() throws UiServiceException {
        try {
            String query = "select id, name, is_enabled enabled from mst_features";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(MasterFeaturesBean.class));
        } catch (Exception e) {
            log.error("Error while fetching features. Details: ", e);
            throw new UiServiceException("Error while fetching the supported features list");
        }
    }

    public List<DateComponentBean> getDateTimeDropdownList() throws UiServiceException {
        try {
            String query = "select d.name label, d.type, d.value from mst_date_component_data d";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(DateComponentBean.class));
        } catch (Exception e) {
            log.error("Error while fetching the data components list. Details: ", e);
            throw new UiServiceException("Error while fetching the data components list");
        }
    }
}


