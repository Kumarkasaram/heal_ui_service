package com.heal.dashboard.service.dao.mysql;


import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.ControllerBean;
import com.heal.dashboard.service.beans.ViewApplicationServiceMappingBean;
import com.heal.dashboard.service.exception.ServerException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class ControllerDao {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public List<Controller> getControllerList(int accountId) throws ServerException {
        try {
            String query = "select id as appId ,name,controller_type_id ,identifier, monitor_enabled  as monitoringEnabled,account_id , status from controller where account_id = ?";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(Controller.class), accountId);
        } catch (DataAccessException e) {
            log.error("Error while fetching controller information", e);
            throw new ServerException("Error in ControllerDao.getControllerList while fetching controller information for accountId : " + accountId);
        }
    }
    
    public Controller getControllerListByAccountIdAndApplicationId(int accountId,int applicationId) throws ServerException {
        try {
            String query = "select id as appId ,name,controller_type_id ,identifier, monitor_enabled  as monitoringEnabled,account_id , status from controller where account_id = ? and id =?";
            return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(Controller.class), accountId,applicationId);
        } catch (DataAccessException e) {
            log.error("Error while fetching controller information", e);
            throw new ServerException("Error in ControllerDao.getControllerList while fetching controller information for accountId : " + accountId);
        }
    }

    public List<Controller> getApplicationsForAccount(int accountId) {
        try {
            String query = "select id as appId, name, controller_type_id, identifier, monitor_enabled as monitoringEnabled, account_id, " +
                    "status from controller where controller_type_id=191 and account_id = ?";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(Controller.class), accountId);
        } catch (DataAccessException e) {
            log.error("Error while fetching applications for accountId [{}]. Details: ", accountId, e);
        }

        return Collections.emptyList();
    }

    public List<ViewApplicationServiceMappingBean> getApplicationServicesByAccount(int accountId) throws ServerException {
        try {
            String query = "select service_id , service_identifier , application_id , application_identifier ,application_name  from view_application_service_mapping where account_id = ?";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(ViewApplicationServiceMappingBean.class), accountId);
        } catch (DataAccessException e) {
            log.error("Error while fetching controller information", e);
            throw new ServerException("Error while fetching view_application_service_mapping information for accountId : " + accountId);
        }
    }


    public List<ControllerBean> getAllServicesForAccount(int accountId) throws ServerException {
        try {
            String query = "select service_id as id , service_name  as name, service_identifier as identifier ,account_id as accountId ,1  as status, 192  as controllerTypeId from view_application_service_mapping where account_id = ?";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(ControllerBean.class), accountId);
        } catch (DataAccessException e) {
            log.error("Error while fetching controller information", e);
            throw new ServerException("Error in ControllerDao.getAllServicesForAccount()  while fetching view_application_service_mapping information for accountId : " + accountId);
        }
    }

    public List<ControllerBean> getServicesByAppId(int applicationId, int accountId) throws ServerException {
        try {
            String query = "select service_id as id , service_name  as name, service_identifier as identifier ,account_id as accountId ,1  as status, 192  as controllerTypeId from view_application_service_mapping where application_id = ?, account_id = ?";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(ControllerBean.class), applicationId, accountId);
        } catch (DataAccessException e) {
            log.error("Error while fetching controller information", e);
            throw new ServerException("Error while fetching view_application_service_mapping information for accountId : " + accountId);
        }
    }

    public List<String> getApplicationNamesBySvcId(int serviceId, int accountId) {
        try {
            String query = "select application_name as name from view_application_service_mapping where service_id = ? and account_id = ?";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(String.class), serviceId, accountId);
        } catch (DataAccessException e) {
            log.error("Error while fetching controller information. Details: ", e);
        }
        return Collections.emptyList();
    }

    public List<ViewApplicationServiceMappingBean> getServicesForApplication(int accountId, String applicationIdentifier) {
        String query = "select service_id serviceId, service_name serviceName, service_identifier serviceIdentifier, " +
                "application_id applicationId, application_name applicationName, application_identifier applicationIdentifier " +
                "from view_application_service_mapping where account_id = ? and application_identifier = ?";

        return jdbcTemplate.query(query, (rs, rowNum) -> ViewApplicationServiceMappingBean.builder().build(), accountId, applicationIdentifier);
    }

    public List<Controller> getApplicationList(int accountId) throws ServerException {
        try {
            String query = "select id as appId,name as name,controller_type_id as controllerTypeId,identifier as identifier,status as status,user_details_id as createdBy,created_time as createdOn, updated_time as updatedTime ,account_id as accountId from controller where account_id = ? and status = 1 and controller_type_id = 191";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(Controller.class), accountId);
        } catch (DataAccessException e) {
            log.error("Error while fetching ApplicationList information", e);
            throw new ServerException("Error in ControllerDao.getApplicationList while fetching ApplicationList information for accountId : " + accountId);
        }
    }

    public List<ControllerBean> getApplicationsBySvcId(int serviceId, int accountId) throws ServerException {
        try {
            String query = "select application_id as id , application_name as name, application_identifier as identifier , 1 as status ,account_id as accountId, 191 as controllerTypeId from view_application_service_mapping where service_id = ? and account_id = ?";
            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(ControllerBean.class), serviceId, accountId);
        } catch (DataAccessException e) {
            log.error("Error while fetching controller information", e);
            throw new ServerException("Error while fetching view_application_service_mapping information for accountId : " + accountId);
        }
    }
}

