package com.heal.dashboard.service.businesslogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.datastax.driver.core.Row;
import com.heal.dashboard.service.beans.AccountBean;
import com.heal.dashboard.service.beans.AccountServiceKey;
import com.heal.dashboard.service.beans.CompInstClusterDetails;
import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.TransactionQueryParams;
import com.heal.dashboard.service.beans.TxnAndGroupBean;
import com.heal.dashboard.service.beans.UserAccessDetails;
import com.heal.dashboard.service.beans.tpf.TFPServiceDetails;
import com.heal.dashboard.service.beans.tpf.TransactionDirection;
import com.heal.dashboard.service.beans.util.RollupTimeMetaData;
import com.heal.dashboard.service.config.ElasticSearchConnectionManager;
import com.heal.dashboard.service.dao.mysql.ComponentInstanceDao;
import com.heal.dashboard.service.dao.mysql.TransactionCassandraDao;
import com.heal.dashboard.service.dao.mysql.TransactionDao;
import com.heal.dashboard.service.exception.ServerException;
import com.heal.dashboard.service.util.CassandraRollupUtility;
import com.heal.dashboard.service.util.Constants;
import com.heal.dashboard.service.util.TopologyUtility;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TFPCommonBL {
	@Autowired
	TopologyUtility topologyUtility;
	@Autowired
	ComponentInstanceDao componentInstanceDao;
	@Autowired
	TransactionCassandraDao transactioncassandraDao;
	@Autowired
	TransactionDao transactionDao;
	
	public List<TFPServiceDetails> getServiceTransactionStats(AccountBean account, Controller application,
			List<Controller> entrySvcs, TransactionDirection direction, UserAccessDetails userAccessDetails,
			List<Controller> serviceList, long fromTime, long toTime) throws ServerException {
		log.trace("{} getServiceTransactionStats().", "invoke method");
		List<TFPServiceDetails> result;
		
		List<Integer> jimEnabledServiceIdList = topologyUtility.getJIMEnabledServiceIdForAccount(account.getAccountId());
		CassandraRollupUtility cassandraRollupUtility = new CassandraRollupUtility();
		cassandraRollupUtility.process(fromTime, toTime, account.getTimezoneMilli());
		List<RollupTimeMetaData> rollupTimeMetaDataList = cassandraRollupUtility.getTimeRanges();
		if (jimEnabledServiceIdList == null)
			jimEnabledServiceIdList = new ArrayList<>();

		long start = System.currentTimeMillis();
		List<Integer> finalJimEnabledServiceIdList = jimEnabledServiceIdList;
		Map<String, TFPServiceDetails> txnDetailsFromElastic = new HashMap<>();
		Boolean receivedDataFromEs = false;
		if (entrySvcs != null && direction != null) {
			// For outbound only as of now
			log.debug("Get details --> {} {} {}", entrySvcs, direction, serviceList);
			txnDetailsFromElastic = ElasticSearchConnectionManager.getInstance().getOutboundSummaryBreakup(entrySvcs,
					serviceList, fromTime, toTime, direction);
			log.debug("Data received from elasticsearch call: {}", txnDetailsFromElastic);
			receivedDataFromEs = true;
		} else {
			log.debug("Get details --> NA NA {}", serviceList);
		}

		Map<String, TFPServiceDetails> finalTxnDetailsFromElastic = txnDetailsFromElastic;
		Boolean finalreceivedDataFromEs = receivedDataFromEs;
		result = serviceList.parallelStream().map(service -> {
			TFPServiceDetails temp = null;
			Boolean fetchFromCassandra = false;
			if (finalTxnDetailsFromElastic == null) {
				fetchFromCassandra = true;
			} else if (finalTxnDetailsFromElastic.containsKey(service.getName()) == false) {
				fetchFromCassandra = true;
			}
			if (fetchFromCassandra == true) {
				log.debug("No data received for {} from elasticsearch", service.getName());
				List<TransactionQueryParams> params = getQueryParams(account.getIdentifier(), service.getIdentifier(),
						rollupTimeMetaDataList);
				List<Row> rowList = fetchTransactionDetails(params);
				temp = processTransactionStats(account, service, application, rowList, fromTime, toTime);
				if (finalreceivedDataFromEs == true) {
					// No data in ES... Coz no Txns from Src -> Dst.. Hence overriding overall
					// volume
					temp.setVolume(0);
				}
			} else {
				temp = finalTxnDetailsFromElastic.get(service.getName());
			}
			temp.setIsJIMEnabled((finalJimEnabledServiceIdList.contains(Integer.parseInt(service.getAppId()))) ? 1 : 0);
			AccountServiceKey key = AccountServiceKey.builder().accountId(account.getAccountId())
					.serviceId(Integer.parseInt(service.getAppId())).build();
			List<CompInstClusterDetails> mappedClusters = componentInstanceDao.getClusterListForService(key.getAccountId(),key.getServiceId());
			int clusterId = 0;
			if (mappedClusters != null && !mappedClusters.isEmpty()) {
				CompInstClusterDetails cluster = mappedClusters.stream().filter(
						it -> it.getStatus() == 1 && !Constants.HOST.equalsIgnoreCase(it.getComponentTypeName()))
						.findAny().orElse(null);
				if (cluster != null) {
					clusterId = cluster.getInstanceId();
					log.debug("Mapped cluster for service: {} is {}.", service.getName(), clusterId);
				} else {
					log.warn("Unable to find any mapped cluster for service: {}. Setting it as 0",
							service.getName());
				}
			}
			temp.setClusterId(clusterId);
			List<String> apps = userAccessDetails.getApplicationIdentifiers();

			if (apps.contains(application.getIdentifier()))
				temp.setUserAccess(true);
			log.debug("Service details: {}", temp);
			return temp;
		}).collect(Collectors.toList());
		log.debug("Time taken to get service transaction stats is {} ms.", (System.currentTimeMillis() - start));

		return result;
	}

	protected List<TransactionQueryParams> getQueryParams(String accountIdentifier, String serviceIdentifier,
			List<RollupTimeMetaData> rollupList) {
		List<TransactionQueryParams> result = new ArrayList<>();
		if (accountIdentifier != null && serviceIdentifier != null) {
			rollupList.forEach(aggLevel -> {
				TransactionQueryParams temp = TransactionQueryParams.builder().accountId(accountIdentifier)
						.serviceId(serviceIdentifier).agentId(Constants.CASSANDRA_ALL_IDENTIFIER)
						.responseType(Constants.TRANSACTION_TYPE_DEFAULT).aggLevel(aggLevel.getAggLevel())
						.fromTime(aggLevel.getFrom()).toTime(aggLevel.getTo()).build();
				result.add(temp);
			});
			log.debug("Optimized number of cassandra queries is {}.", result.size());
			return result;
		}
		log.warn("Unable to fetch optimizied set of cassandra query params.");
		return result;

	}
	protected List<Row> fetchTransactionDetails(List<TransactionQueryParams> queryParams) {
        List<Row> rowList = new ArrayList<>();
        long start = System.currentTimeMillis();
        queryParams.forEach( param -> {
                rowList.addAll(transactioncassandraDao.getServiceWiseTransactionVolume(param));
        });
        log.debug("Time taken to fetch {} rows using {} queries is {} ms.",rowList.size(), queryParams.size(),
                (System.currentTimeMillis()-start));
        return rowList;
    }
	
	
	protected TFPServiceDetails processTransactionStats(AccountBean account, Controller service, Controller application,
			List<Row> rowList, long fromTime, long toTime) {
		long start;
		long[] volume = new long[1];
		int[] txnCount = new int[1];
		int[] failCount = new int[1];
		int[] slowCount = new int[1];
		Map<String, Integer> volumeMap = new HashMap<>();
		Map<String, Integer> slowMap = new HashMap<>();
		Map<String, Integer> failMap = new HashMap<>();

		List<TxnAndGroupBean> mappedTransactionList = getTransactionForApplication(account.getAccountId(),
				Integer.parseInt(service.getAppId()));

		start = System.currentTimeMillis();
		rowList.forEach(row -> {
			Map<String, Integer> data = row.getMap("total", String.class, Integer.class);
			data.forEach((key, value) -> volumeMap.merge(key, value, Integer::sum));

			Map<String, Integer> fail = row.getMap("fail", String.class, Integer.class);
			fail.forEach((key, value) -> failMap.merge(key, value, Integer::sum));

			Map<String, Integer> slow = row.getMap("slow", String.class, Integer.class);
			slow.forEach((key, value) -> slowMap.merge(key, value, Integer::sum));

		});
		log.debug("Time taken to merge all rows is {} ms.", (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		Set<String> txnIdentifierSet = mappedTransactionList.stream().map(TxnAndGroupBean::getIdentifier)
				.collect(Collectors.toSet());
		volumeMap.forEach((k, v) -> {
			if (txnIdentifierSet.contains(k)) {
				txnCount[0]++;
				volume[0] += v;
			}
		});

		failMap.forEach((k, v) -> {
			if (txnIdentifierSet.contains(k)) {
				failCount[0] += v;
			}
		});

		slowMap.forEach((k, v) -> {
			if (txnIdentifierSet.contains(k)) {
				slowCount[0] += v;
			}
		});

		log.debug("Time taken to calculate volume of all txn is {} ms.", (System.currentTimeMillis() - start));

		return TFPServiceDetails.builder().applicationName(application.getName()).serviceName(service.getName())
				.serviceId(Integer.parseInt(service.getAppId())).transactionCount(txnCount[0]).volume(volume[0])
				.isSlow(slowCount[0]).isFailed(failCount[0]).build();
	}
	
	
	   public  List<TxnAndGroupBean> getTransactionForApplication(int accId, int appId) {
	        long start = System.currentTimeMillis();
	        List<TxnAndGroupBean> txnList = transactionDao.getTxnAndGroupForServiceAndAccount(accId,appId);
	        log.debug("Time taken to fetch txn's for acc: {} ,service: {} is {} ms.", accId, appId,
	                (System.currentTimeMillis() - start));
	        return txnList;
	    }

}
