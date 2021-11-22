package com.heal.dashboard.service.dao.mysql;

import com.heal.dashboard.service.beans.*;
import com.heal.dashboard.service.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
@Repository
public class TimezoneDao {
	@Autowired
	JdbcTemplate jdbcTemplate;

	public UserAttributeBeen getUserAttributes(String username) {
		try {
			String query = "select id id, user_identifier userIdentifier, contact_number contactNumber, email_address emailAddress, username username, "
					+ "user_details_id userDetailsId, created_time createdTime, updated_time updatedTime, status status, is_timezone_mychoice isTimezoneMychoice, "
					+ "mst_access_profile_id mstAccessProfileId, mst_role_id mstRoleId, is_notifications_timezone_mychoice isNotificationsTimezoneMychoice " +
					"from user_attributes where username= ?";
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(UserAttributeBeen.class), username);
		} catch (Exception e) {
			log.error("User details unavailable for username [{}]. Details: ", username, e);
		}
		
		return null;
	}

	public UserDetailsBean getUsers(String identifier) {
		try {
			String query = "SELECT u.id id, u.user_identifier userIdentifier, u.username userName, u.user_details_id createdBy, a.name userProfile,"
					+ " r.name role, u.status, u.created_time createdOn FROM user_attributes u, mst_roles r, mst_access_profiles a "
					+ " where u.mst_access_profile_id = a.id and u.mst_role_id = r.id and a.mst_role_id = r.id and u.user_identifier= ?";
			
			return jdbcTemplate.queryForObject(query, BeanPropertyRowMapper.newInstance(UserDetailsBean.class), identifier);
		} catch (Exception e) {
			log.error("User details unavailable for identifier [{}]. Details: ", identifier, e);
		}
		
		return null;
	}

	public TimezoneDetail getTimezonesById(String id) {
		try {
			String query = "select id, time_zone_id timeZoneId, timeoffset timeOffset, created_time createdTime, updated_time updatedTime, " +
					"user_details_id userDetailsId, account_id accountId, offset_name offsetName from mst_timezone where id = ? and status = 1";
			return jdbcTemplate.queryForObject(query, BeanPropertyRowMapper.newInstance(TimezoneDetail.class), id);
		} catch (Exception e) {
			log.error("Timezone details unavailable for id [{}]. Details: ", id, e);
		}
		
		return null;
	}

	public int getUserTagMappingId(String objectRefTable, int objectId, int tagId) {
		try {
			String query = "select id from tag_mapping where object_ref_table = ? and object_id= ? and tag_id= ?";
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(Integer.class), objectRefTable, objectId, tagId);
		} catch (Exception e) {
			log.error("Invalid input parameter/s provided. Details: objectRefTable [{}], objectId [{}] and tagId [{}]. Details: ", objectRefTable, objectId, tagId, e);
			return 0;
		}
	}

	public TimezoneDetail getTimezoneByUser(String objectRefTable, int objectId, int tagId) {
		try {
			String query = "select mtz.id, mtz.time_zone_id timeZoneName, mtz.timeoffset timeOffset, mtz.created_time createdTime, mtz.updated_time updatedTime, mtz.user_details_id userDetailsId, mtz.account_id accountId, mtz.offset_name offsetName, mtz.abbreviation abbreviation " +
		            "from mst_timezone mtz, tag_mapping tm where mtz.id=tm.tag_key and tm.object_ref_table=? and tm.object_id=? and tm.tag_id=? and mtz.status=1";
			return jdbcTemplate.queryForObject(query, new BeanPropertyRowMapper<>(TimezoneDetail.class), objectRefTable, objectId, tagId);
		} catch (Exception e) {
			log.error("Invalid input parameter/s provided. Details: objectRefTable [{}], objectId [{}] and tagId [{}]. Details: ", objectRefTable, objectId, tagId, e);
			return null;
		}
	}



	public TagDetails getTagDetails(String name, int accountId) {
		try {
			String query = "select id,name,tag_type_id tagTypeId,is_predefined isPredefined,ref_table refTable,created_time createdTime,updated_time updatedTime,"
					+ "account_id accountId,user_details_id userDetailsId,ref_where_column_name refWhereColumnName,ref_select_column_name refSelectColumnName "
					+ "from tag_details where name = ? and account_id = ?";
			return jdbcTemplate.queryForObject(query, BeanPropertyRowMapper.newInstance(TagDetails.class), name, accountId);
		} catch (Exception e) {
			log.error("Tag details unavailable for name [{}] and account [{}]. Details: ", name, accountId, e);
		}
		
		return null;
	}

	public int updateUserTimezoneChoice(int isTimezoneMychoice, int isNotificationsTimezoneMychoice, String timeInGMT, String username, String updatingUserIdentifier) {
		try {
			String query = "UPDATE user_attributes SET is_timezone_mychoice=?, is_notifications_timezone_mychoice=?,"
					+ "updated_time= ?, user_details_id= ? where username= ?";
			return jdbcTemplate.update(query, isTimezoneMychoice, isNotificationsTimezoneMychoice, timeInGMT, updatingUserIdentifier, username);
		} catch (Exception e) {
			log.error("failed to update data in user_attributes for username [{}]. Details: ", username, e);
		}
		return 0;
	}

	public int deleteUserTagMapping(int id) {
		try {
			String query = "delete from tag_mapping where id=?";
			return jdbcTemplate.update(query, id);
		} catch (Exception e) {
			log.error("Error while deleting tag mapping for id [{}]. Details: ", id, e);
		}

		return 0;
	}

	public int updateUserTagMapping(int tagKey, String updatedTime, int id, String userDetailsId) {
		try {
			String query = "UPDATE tag_mapping set tag_key=?, updated_time=?, user_details_id=? where id=?";
			return jdbcTemplate.update(query, tagKey, updatedTime, userDetailsId, id);
		} catch (Exception e) {
			log.error("Error while updating tag mapping entries for id [{}]. Details: ", id, e);
		}

		return 0;
	}

	public int addUserTagMapping(TagMapping tagMappingDetails) {
		try {
			String query = "insert into tag_mapping (tag_id,object_id,object_ref_table,tag_key,tag_value,created_time,updated_time,account_id,user_details_id) values (?,?,?,?,?,?,?,?,?)";
			return jdbcTemplate.update(query, new PreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps) throws SQLException {
					String timeInGMT = DateUtil.getTimeInGMT(System.currentTimeMillis());
					ps.setInt(1, tagMappingDetails.getTagId());
					ps.setInt(2, tagMappingDetails.getObjectId());
					ps.setString(3, tagMappingDetails.getObjectRefTable());
					ps.setString(4, tagMappingDetails.getTagKey());
					ps.setString(5, tagMappingDetails.getTagValue());
					ps.setString(6, timeInGMT);
					ps.setString(7, timeInGMT);
					ps.setInt(8, tagMappingDetails.getAccountId());
					ps.setString(9, tagMappingDetails.getUserDetailsId());
				}
			});
		} catch (Exception e) {
			log.error("Error while adding tag mapping entries with tagMapping details [{}]. Details: ", tagMappingDetails, e);
		}

		return 0;
	}
}
