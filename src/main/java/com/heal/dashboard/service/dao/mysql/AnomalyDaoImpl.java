package com.heal.dashboard.service.dao.mysql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.heal.dashboard.service.beans.CassandraQueryParam;
import com.heal.dashboard.service.config.CassandraConnectionManager;
import com.heal.dashboard.service.util.Constants;

@Component
public class AnomalyDaoImpl extends CommonCassandraDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnomalyDaoImpl.class);
    private static Session session = null;
    private static final String ANOMALY_ENRICH_TABLE_NAME = "anomalies_enrich_details";
    private static final String ACCOUNT_ID_LITERAL = "account_id";
    private static final String CONTROLLER_ID_LITERAL = "controller_id";
    private static final String INSTANCE_ID_LITERAL = "instance_id";
    private static final String CATEGORY_ID_LITERAL = "category_id";
    private static final String KPI_ID_LITERAL = "kpi_id";
    private static final String KPI_ATTRIBUTE_LITERAL = "kpi_attribute";
    private static final String ANOMALY_TIME_LITERAL = "anomaly_time";
    private static final String ANOMALY_ID = "anomaly_id";

    public AnomalyDaoImpl() {
        session = CassandraConnectionManager.getSession();
    }

    /**
     * The below constructor can be used for unit testing when we want to mock cassandra connection.
     * @param session
     */
    public AnomalyDaoImpl(Session session) {
        AnomalyDaoImpl.session = session;
    }

    public long getAnomalyCountForCategory(String accountId, String serviceId, String instanceId, String categoryId,
                                           String kpiId, String kpiAttributeId, long fromTime, long toTime) {
        LOGGER.trace("{} getAnomalyCountForCategory().", Constants.INVOKED_METHOD);
        long violations = 0L;
        long start = System.currentTimeMillis();
        List<Row> rowList = getAnomalyEnrichDetails(accountId, serviceId, instanceId, categoryId, kpiId, kpiAttributeId,
                fromTime, toTime);
        for(Row row: rowList) {
            Set<String> anomalies = row.getSet("anomalies", String.class);
            violations += anomalies.size();
        }
        LOGGER.debug("Anomaly count: {}, Time taken to get anomaly count: {}", violations,
                (System.currentTimeMillis() - start));
        return violations;
    }

    public List<Row> getAnomalyEnrichDetails(String accountId, String serviceId, String instanceId, String categoryId,
                                             String kpiId, String kpiAttributeId, long fromTime, long toTime) {
        LOGGER.trace("{} getAnomalyEnrichDetails().", Constants.INVOKED_METHOD);
        BuiltStatement builtStatement = QueryBuilder.select().all()
                .from(ANOMALY_ENRICH_TABLE_NAME)
                .where(QueryBuilder.eq(ACCOUNT_ID_LITERAL, accountId))
                .and(QueryBuilder.eq(CONTROLLER_ID_LITERAL, serviceId))
                .and(QueryBuilder.eq(INSTANCE_ID_LITERAL, instanceId))
                .and(QueryBuilder.eq(CATEGORY_ID_LITERAL, categoryId))
                .and(QueryBuilder.eq(KPI_ID_LITERAL, kpiId))
                .and(QueryBuilder.eq(KPI_ATTRIBUTE_LITERAL, kpiAttributeId))
                .and(QueryBuilder.gte(ANOMALY_TIME_LITERAL, fromTime))
                .and(QueryBuilder.lt(ANOMALY_TIME_LITERAL, toTime));

        LOGGER.debug("Query for getting anomaly count for category: {}", builtStatement);
        long start = System.currentTimeMillis();
        checkConnection(session);
        ResultSet resultSet = session.execute(builtStatement);
        List<Row> rowList = resultSet.all();
        LOGGER.debug("{} ms was taken to execute: {}", (System.currentTimeMillis() - start), builtStatement);
        return rowList;
    }

    public Row getAnomalyUsingId(String anomalyId) {
        LOGGER.trace("{} getAnomalyUsingId(). anomaly id: {}",Constants.INVOKED_METHOD, anomalyId);
        BuiltStatement builtStatement = QueryBuilder.select().all()
                .from("anomalies")
                .where(QueryBuilder.eq(ANOMALY_ID, anomalyId));

        LOGGER.debug("Query:");
        long start = System.currentTimeMillis();
        checkConnection(session);
        ResultSet resultSet = session.execute(builtStatement);
        Row row = resultSet.one();
        LOGGER.debug("{} ms was taken to execute: {}", (System.currentTimeMillis() - start), builtStatement);
        return row;
    }

    public long getTransactionsCountForCluster(String accountId, String serviceId, long fromTime, long toTime) {
        LOGGER.trace("{} getTransactionsCountForCluster().", Constants.INVOKED_METHOD);
        List<Integer> aggLevels = new ArrayList<>(Arrays.asList(1,15,30,45,60,1440));
        BuiltStatement builtStatement = QueryBuilder.select().countAll()
                .from("service_transactions_data")
                .where(QueryBuilder.eq(ACCOUNT_ID_LITERAL, accountId))
                .and(QueryBuilder.eq("service_id", serviceId))
                .and(QueryBuilder.eq("response_type", "DC"))
                .and(QueryBuilder.in("agg_level", aggLevels))
                .and(QueryBuilder.gte("time", fromTime))
                .and(QueryBuilder.lt("time", toTime));

        LOGGER.debug("Query for getting transactions count for cluster: {}", builtStatement);
        long start = System.currentTimeMillis();
        checkConnection(session);
        ResultSet resultSet = session.execute(builtStatement);
        List<Row> rowList = resultSet.all();
        LOGGER.debug("{} ms was taken to execute: {}", (System.currentTimeMillis() - start), builtStatement);
        return rowList.get(0).getLong("count");
    }

    public long getTransactionsCountForInstance(String accountId, String agentId, long fromTime, long toTime) {
        LOGGER.trace("{} getTransactionsCountForInstance().", Constants.INVOKED_METHOD);
        List<Integer> aggLevels = new ArrayList<>(Arrays.asList(1,15,30,45,60,1440));
        BuiltStatement builtStatement = QueryBuilder.select().countAll()
                .from("agent_transactions_data")
                .where(QueryBuilder.eq(ACCOUNT_ID_LITERAL, accountId))
                .and(QueryBuilder.eq("agent_id", agentId))
                .and(QueryBuilder.eq("response_type", "DC"))
                .and(QueryBuilder.in("agg_level", aggLevels))
                .and(QueryBuilder.gte("time", fromTime))
                .and(QueryBuilder.lt("time", toTime));

        LOGGER.debug("Query for getting transactions count for instance: {}", builtStatement);
        long start = System.currentTimeMillis();
        checkConnection(session);
        ResultSet resultSet = session.execute(builtStatement);
        List<Row> rowList = resultSet.all();
        LOGGER.debug("{} ms was taken to execute: {}", (System.currentTimeMillis() - start), builtStatement);
        return rowList.get(0).getLong("count");
    }

    public Set<String> getAnomalyEnrichDetails(CassandraQueryParam param) {
        LOGGER.trace("{} getAnomalyEnrichDetails().", Constants.INVOKED_METHOD);
        BuiltStatement builtStatement = QueryBuilder.select().all()
                .from(ANOMALY_ENRICH_TABLE_NAME)
                .where(QueryBuilder.eq(ACCOUNT_ID_LITERAL, param.getAccountIdentifier()))
                .and(QueryBuilder.eq(CONTROLLER_ID_LITERAL, param.getServiceIdentifier()))
                .and(QueryBuilder.eq(INSTANCE_ID_LITERAL, param.getInstanceIdentifier()))
                .and(QueryBuilder.eq(CATEGORY_ID_LITERAL, param.getCategoryIdentifier()))
                .and(QueryBuilder.eq(KPI_ID_LITERAL, param.getKpiId()))
                .and(QueryBuilder.eq(KPI_ATTRIBUTE_LITERAL, param.getKpiAttributeId()))
                .and(QueryBuilder.gte(ANOMALY_TIME_LITERAL, param.getFromTime()))
                .and(QueryBuilder.lt(ANOMALY_TIME_LITERAL, param.getToTime()));

        LOGGER.debug("Query for getting anomaly count for category: {}", builtStatement);
        long start = System.currentTimeMillis();
        checkConnection(session);
        ResultSet resultSet = session.execute(builtStatement);
        Set<String> rowSet = resultSet.all()
                .parallelStream()
                .map( row -> row.getSet(Constants.CASSANDRA_COLOUMN_ANOMALY_ID_SET, String.class))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        LOGGER.debug("{} ms was taken to execute: {}", (System.currentTimeMillis() - start), builtStatement);
        return rowSet;
    }
}
