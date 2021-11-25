package com.heal.dashboard.service.config;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.heal.dashboard.service.beans.Controller;
import com.heal.dashboard.service.beans.tpf.TFPAnomalyTransactionDetails;
import com.heal.dashboard.service.beans.tpf.TFPServiceDetails;
import com.heal.dashboard.service.beans.tpf.TransactionDirection;
import com.heal.dashboard.service.util.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Configuration
public class ElasticSearchConnectionManager {

	 	@Value("${elasticsearch.host:127.0.0.1}")
	    private static String host;

	    @Value("${elasticsearch.port:9200}")
	    private static int port;
	    
	    @Value("${elasticsearch.protocol:http}")
	    private static String protocall;
	    
	    
	    private static final String ELASTICSEARCH_HOST = host;
	    private static final Integer ELASTICSEARCH_PORT = port;
	    private static final String ELASTICSEARCH_PROTOCOL = protocall;
	    private static RestHighLevelClient elasticClient = null;

	    public ElasticSearchConnectionManager()    {
	    }

	    private static ElasticSearchConnectionManager _this;
	    public static ElasticSearchConnectionManager getInstance(){
	        synchronized (ElasticSearchConnectionManager.class) {
	            if (_this == null)  {
	                _this = new ElasticSearchConnectionManager();
	                _this.pingElasticSearch();
	            }
	        }

	        return _this;
	    }

	    private boolean pingElasticSearch()  {
	        boolean attemptConnect = false;
	        try {
	            if (elasticClient == null)  {
	                attemptConnect = true;
	            } else if (!elasticClient.ping(org.elasticsearch.client.RequestOptions.DEFAULT)) {
	                attemptConnect = true;
	            }
	            if (!attemptConnect) {
	                return true;
	            }
	        } catch (IOException io)    {
	            attemptConnect = true;
	            log.error("IO Exception when connection to elasticsearch {}://{}:{} - {}", ELASTICSEARCH_PROTOCOL, ELASTICSEARCH_HOST, ELASTICSEARCH_PORT, io);
	        }
	        if (attemptConnect == true) {
	            log.info("Attempting to initialize elasticsearch client connecting to {}://{}:{}", ELASTICSEARCH_PROTOCOL, ELASTICSEARCH_HOST, ELASTICSEARCH_PORT);
	            elasticClient = new RestHighLevelClient(RestClient.builder(
	                    new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT, ELASTICSEARCH_PROTOCOL)));
	            return true;
	        }
	        return false;
	    }

	    
	    
	    public Map<String, TFPServiceDetails> getOutboundSummaryBreakup(List<Controller> entryPtSvcs, List<Controller> outboundSvcs, Long from, Long to, TransactionDirection direction)    {
	        log.debug("Get Outbound Summary for {} -> {}", entryPtSvcs, outboundSvcs);
	        long start = System.currentTimeMillis();
	        List<MatchPhraseQueryBuilder> matchSourceServices = new ArrayList<>();
	        List<MatchPhraseQueryBuilder> matchDestServices = new ArrayList<>();
	        Map<String, TFPServiceDetails> data = new HashMap<>();
	        if (!pingElasticSearch())    {
	            return data;
	        }
	        String[] indices = new String[entryPtSvcs.size() * outboundSvcs.size()];
	        int ctr = 0;
	        Map<String,Integer> svcIds = new HashMap<>();
	        for(Controller srcController : entryPtSvcs) {
	            for(Controller dstController : outboundSvcs)    {
	                String index = Constants.TFP_INDEX_PREFIX + srcController.getName().toLowerCase() + "-" + dstController.getName().toLowerCase() + "*";
	                indices[ctr] = index;
	                ctr++;
	                MatchPhraseQueryBuilder mpSrc = QueryBuilders
	                        .matchPhraseQuery(Constants.TFP_PEER_SERVICE, dstController.getName());
	                matchDestServices.add(mpSrc);
	                svcIds.put(dstController.getName(), Integer.valueOf(dstController.getAppId()));
	            }
	            MatchPhraseQueryBuilder mpSrc = QueryBuilders
	                    .matchPhraseQuery(Constants.TFP_SERVICE, srcController.getName());
	            matchSourceServices.add(mpSrc);
	            svcIds.put(srcController.getName(), Integer.valueOf(srcController.getAppId()));
	        }
	        log.debug("Elastic Indices: {} ", Arrays.toString(indices));

	        MatchPhraseQueryBuilder mpDirection = QueryBuilders
	                .matchPhraseQuery(Constants.TFP_TXN_DIRECTION, direction.toString());

	        BoolQueryBuilder boolQueryBuilderSrcSvcz = QueryBuilders.boolQuery();
	        for (MatchPhraseQueryBuilder matchPhraseQueryBuilder : matchSourceServices) {
	            boolQueryBuilderSrcSvcz.should(matchPhraseQueryBuilder);
	        }
	        boolQueryBuilderSrcSvcz.minimumShouldMatch(1);

	        BoolQueryBuilder boolQueryBuilderDstSvcz = QueryBuilders.boolQuery();
	        for (MatchPhraseQueryBuilder matchPhraseQueryBuilder : matchDestServices) {
	            boolQueryBuilderDstSvcz.should(matchPhraseQueryBuilder);
	        }
	        boolQueryBuilderDstSvcz.minimumShouldMatch(1);
	        org.elasticsearch.index.query.RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(Constants.ELASTIC_TIMESTAMP).gte(new Date(from)).lte(new Date(to));

	        // Ignore unmatched Transactions
	        MatchPhraseQueryBuilder unmatchedTxns = QueryBuilders.matchPhraseQuery(Constants.TFP_TXN_ID, Constants.TFP_UNMATCHED_TXN);

	        Scroll scroll = new Scroll(org.elasticsearch.common.unit.TimeValue.timeValueMinutes(1L));

	        SearchRequest searchRequest = new SearchRequest(indices);
	        searchRequest.searchType(SearchType.QUERY_THEN_FETCH);
	        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

	        boolQueryBuilder.must(mpDirection);
	        boolQueryBuilder.must(rangeQueryBuilder);
	        boolQueryBuilder.must(boolQueryBuilderDstSvcz);
	        boolQueryBuilder.must(boolQueryBuilderSrcSvcz);
	        boolQueryBuilder.mustNot(unmatchedTxns);

	        // Gets Total Volume
	        SumAggregationBuilder sumBuilder = AggregationBuilders.sum(Constants.TFP_TXN_VOLUME).field(Constants.TFP_TXN_VOLUME );
	        // Gets total unique Txns
	        CardinalityAggregationBuilder carBuilder = AggregationBuilders.cardinality(Constants.TFP_TXN_ID).field(Constants.TFP_TXN_ID + ".keyword" );

	        TermsAggregationBuilder dirBuilder = AggregationBuilders.terms(Constants.TFP_TXN_DIRECTION).field(Constants.TFP_TXN_DIRECTION + ".keyword");
	        TermsAggregationBuilder pSvcBuilder = AggregationBuilders.terms(Constants.TFP_PEER_SERVICE).field(Constants.TFP_PEER_SERVICE + ".keyword");
	        TermsAggregationBuilder svcBuilder = AggregationBuilders.terms(Constants.TFP_SERVICE).field(Constants.TFP_SERVICE + ".keyword");
	        TermsAggregationBuilder termsStatus = AggregationBuilders.terms(Constants.TFP_TXN_RESPONSES_STATUS_TAG).field(Constants.TFP_TXN_RESPONSES_STATUS_TAG + ".keyword");

	        termsStatus.subAggregation(sumBuilder);
	        dirBuilder.subAggregation(carBuilder);
	        dirBuilder.subAggregation(termsStatus);
	        pSvcBuilder.subAggregation(dirBuilder);
	        svcBuilder.subAggregation(pSvcBuilder);

	        SearchSourceBuilder searchRequestBuilder = new SearchSourceBuilder();
	        searchRequestBuilder.query(boolQueryBuilder);
	        searchRequestBuilder.size(1000);
	        searchRequestBuilder.aggregation(svcBuilder);
	        searchRequest.scroll(scroll);

	        searchRequest.source(searchRequestBuilder);
	        searchRequest.searchType(SearchType.QUERY_THEN_FETCH);
	        log.debug("Request to Elastic: {}", searchRequest);
	        SearchResponse searchResponse;

	        try {
	            searchResponse = elasticClient.search(searchRequest, RequestOptions.DEFAULT);
	            //LOGGER.debug("Received response from Elasticsearch");
	            log.debug("Received response from Elasticsearch: {}", searchResponse);
	            if (null == searchResponse.getAggregations())   {
	            	log.debug("No Aggregations in Response!");
	                return data;
	            }
	            ParsedStringTerms aggrsSvc = searchResponse.getAggregations().get(Constants.TFP_SERVICE);

	            Map<String, TFPAnomalyTransactionDetails> txnWiseDetails = new HashMap<>();

	            for (Bucket bucketTxn : aggrsSvc.getBuckets()) {
	                String svcName = String.valueOf(bucketTxn.getKey());

	                ParsedStringTerms aggrsVol = bucketTxn.getAggregations().get(Constants.TFP_PEER_SERVICE);
	                if (aggrsVol != null)   {
	                    for (Bucket bucketVol : aggrsVol.getBuckets()) {
	                        String peerSvcName = String.valueOf(bucketVol.getKey());
	                        ParsedStringTerms aggrs = bucketVol.getAggregations().get(Constants.TFP_TXN_DIRECTION);
	                        if (aggrs != null)  {
	                            for (Bucket bucketDir : aggrs.getBuckets()) {
	                                String directionName = String.valueOf(bucketDir.getKey());
	                                double overallVolume = 0;
	                                double slowVolume = 0;
	                                double failedVolume = 0;
	                                long uniqueTxns = 0;
	                                ParsedCardinality txns = bucketDir.getAggregations().get(Constants.TFP_TXN_ID);
	                                if (txns != null)   {
	                                    uniqueTxns = txns.getValue();
	                                }
	                                ParsedStringTerms aggStatus = bucketDir.getAggregations().get(Constants.TFP_TXN_RESPONSES_STATUS_TAG);
	                                if (aggStatus != null)  {
	                                    for (Bucket bucket : aggStatus.getBuckets())  {
	                                        ParsedSum volume = bucket.getAggregations().get(Constants.TFP_TXN_VOLUME);
	                                        String txnStatus = String.valueOf(bucket.getKey());
	                                        if("SLOW".equalsIgnoreCase(txnStatus))  {
	                                            slowVolume = volume.getValue();
	                                        }
	                                        else if ("FAIL".equalsIgnoreCase(txnStatus))  {
	                                            failedVolume = volume.getValue();
	                                        }
	                                        overallVolume = overallVolume + volume.getValue();
	                                    }
	                                }
	                                TFPServiceDetails svcDtls = TFPServiceDetails.builder()
	                                        .applicationName(svcName)
	                                        .transactionCount((int) uniqueTxns)
	                                        .volume((long) overallVolume)
	                                        .isSlow((int) slowVolume)
	                                        .isFailed((int) failedVolume)
	                                        .serviceId(svcIds.get(peerSvcName))
	                                        .serviceName(peerSvcName)
	                                        .build();
	                                log.debug("{} -> {} Total Volume: {}", svcName, peerSvcName, overallVolume);
	                                data.put(peerSvcName, svcDtls);
	                            }
	                        }
	                        else {
	                        	log.info("No aggrs buckets - No aggregations on Response Time");
	                        }
	                    }
	                }
	                else {
	                	log.info("No aggrsVol buckets - No aggregations on Volume");
	                }
	            }
	        } catch (IOException e) {
	        	log.error("IO Exception when fetching response from elasticsearch for request {} : {}", searchRequest, e);
	        }
	        log.debug("Time taken to get TFP Outbound {} ms.",(System.currentTimeMillis()-start));


	        return data;
	    }

}
