package com.heal.dashboard.service.dao.mysql;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.heal.dashboard.service.beans.TransactionQueryParams;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class TransactionCassandraDao {

	@Autowired
	CassandraOperations cassandraTemplate;

	private static final String COLUMN_AGG_LEVEL = "agg_level";
	private static final String COLUMN_ACCOUNT_ID = "account_id";
	private static final String COLUMN_RESPONSE_TYPE = "response_type";
	private static final String COLUMN_TIME = "time";
	private static final String COLUMN_SERVICE_ID = "service_id";
	private static final String TABLE_SERVICE_TRANSACTIONS = "service_transactions_data";

	public List<Row> getServiceWiseTransactionVolume(TransactionQueryParams params) {

		BuiltStatement builtStatement = (QueryBuilder.select().all().from(TABLE_SERVICE_TRANSACTIONS)
				.where(QueryBuilder.eq(COLUMN_AGG_LEVEL, params.getAggLevel()))
				.and(QueryBuilder.eq(COLUMN_ACCOUNT_ID, params.getAccountId()))
				.and(QueryBuilder.eq(COLUMN_SERVICE_ID, params.getServiceId()))
				.and(QueryBuilder.eq(COLUMN_RESPONSE_TYPE, params.getResponseType()))
				.and(QueryBuilder.gte(COLUMN_TIME, params.getFromTime()))
				.and(QueryBuilder.lte(COLUMN_TIME, params.getToTime())));

		log.debug("Transaction service aggregated query: {}", builtStatement);
		List<Row> rowList = cassandraTemplate.select(builtStatement.toString(), Row.class);
		return rowList;
	}

}
