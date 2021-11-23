package com.heal.dashboard.service.dao.mysql;

import com.heal.dashboard.service.beans.TxnAndGroupBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class TransactionDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<TxnAndGroupBean> getTxnAndGroupForServiceAndAccount(int accountId, int serviceId) {
        try {
            String query = "select t.id as txnId, tm.tag_key as serviceId, t.name as txnName, t.status, t.audit_enabled as isAutoEnabled, " +
                    "t.is_autoconfigured as isAutoConfigured, t.user_details_id as userDetailsId, m.name as transactionTypeName, " +
                    "t.pattern_hashcode as patternHashCode, t.description, t.identifier, t.account_id as accountId, " +
                    "t.is_business_txn as isBusinessTransaction, (select group_concat(txn_group_id) " +
                    "from transaction_group_mapping where object_id=t.id and object_ref_table='transaction') " +
                    "as tagListString from transaction t, tag_mapping tm, mst_sub_type m where t.status=1 " +
                    "and t.monitor_enabled = 1 and t.id = tm.object_id and tm.tag_id=1 and tm.object_ref_table ='transaction' " +
                    "and tm.account_id = t.account_id and m.id = t.transaction_type_id and t.account_id = ? and and tm.tag_key = ?";

            return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(TxnAndGroupBean.class), accountId, serviceId);
        } catch (DataAccessException e) {
            log.error("Exception occurred while fetching transaction details for serviceId [{}] and accountId [{}]", serviceId, accountId);
        }

        return Collections.emptyList();
    }
    
}
