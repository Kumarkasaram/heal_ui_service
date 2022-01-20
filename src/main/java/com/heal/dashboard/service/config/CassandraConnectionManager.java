package com.heal.dashboard.service.config;

import com.appnomic.appsone.api.util.ConfProperties;
import com.appnomic.appsone.api.common.Constants;
import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CassandraConnectionManager {
    private static final String nodesKey = "nodes";
    private static final String keyspaceKey = "keyspace_names";
    private static final String usernameKey = "username";
    private static final String authKey = "password";
    private static final String connectionpoolKey = "maxConnections";
    private static final String sslEnabled = "sslEnabled";
    private static final String reconnectDelay = "reconnectDelay";
    private static final String readTimeout = "readTimeout";
    private static Session cassandraSession = null;
    private static final Logger logger = LoggerFactory.getLogger(CassandraConnectionManager.class);

    public static Session getSession() {
        Map<String,Object> cassandraConfigs = getCassandraConfigParams();
        List<InetSocketAddress> nodes = (List<InetSocketAddress>) cassandraConfigs.get(nodesKey);
        String cassandraKeySpaceName = (String) cassandraConfigs.get(keyspaceKey);
        String cassandraUsername = (String) cassandraConfigs.get(usernameKey);
        String cassandraPassword = (String) cassandraConfigs.get(authKey);
        int cassandraConnectionpoolSize = Integer.valueOf((String) cassandraConfigs.get(connectionpoolKey));
        long reconnectInMs = Long.valueOf((String)cassandraConfigs.get(reconnectDelay));
        int readTimeoutMs = Integer.valueOf((String) cassandraConfigs.get(readTimeout));
        boolean isSslEnabled = Boolean.parseBoolean((String)cassandraConfigs.get(sslEnabled));


        try {
            if(cassandraSession == null || cassandraSession.isClosed())    {
                logger.debug("Attempting cassandra connection.");
                PoolingOptions poolingOptions = new PoolingOptions().setMaxConnectionsPerHost(HostDistance.REMOTE, cassandraConnectionpoolSize);
                ReconnectionPolicy reconnectionPolicy = new ConstantReconnectionPolicy(reconnectInMs);
                if(isSslEnabled){
                    cassandraSession = Cluster.builder()
                            .addContactPointsWithPorts(nodes)
                            .withCredentials(cassandraUsername, cassandraPassword)
                            .withPoolingOptions(poolingOptions)
                            .withSSL()
                            .withReconnectionPolicy(reconnectionPolicy)
                            .build()
                            .init()
                            .connect(cassandraKeySpaceName);

                } else {
                    cassandraSession = Cluster.builder()
                            .addContactPointsWithPorts(nodes)
                            .withCredentials(cassandraUsername, cassandraPassword)
                            .withPoolingOptions(poolingOptions)
                            .build()
                            .init()
                            .connect(cassandraKeySpaceName);
                }
            }
            SocketOptions socketOptions = cassandraSession.getCluster().getConfiguration().getSocketOptions();
            socketOptions.setReadTimeoutMillis(readTimeoutMs);
            logger.info("Cassandra details: nodes:{}, isSSLEnabled:{}, connect status: {}.", nodes, isSslEnabled, ! cassandraSession.isClosed());
            logger.debug("Cassandra connect TO: {} ms, read TP: {} ms.",cassandraSession.getCluster().getConfiguration().getSocketOptions().getConnectTimeoutMillis(),
                    cassandraSession.getCluster().getConfiguration().getSocketOptions().getReadTimeoutMillis());
        } catch (Exception e) {
            logger.error("Unable to get a connection to cassandra.",e);
            return null;
        }
        return cassandraSession;
    }

    private static Map<String,Object> getCassandraConfigParams() {

        String cassandraKeyspaceName = ConfProperties.getString(Constants.CASSANDRA_SERVER_SCHEMA_PROPERTY_NAME,
                Constants.CASSANDRA_SERVER_DEFAULT_SCHEMA);
        String cassandraIpPortList = ConfProperties.getString(Constants.CASSANDRA_NODES_PROPERTY_NAME,
                Constants.CASSANDRA_NODES_DEFAULT_VALUE);
        String cassandraUsername = ConfProperties.getString(Constants.CASSANDRA_SERVER_USERNAME_PROPERTY_NAME,
                Constants.CASSANDRA_SERVER_USERNAME_DEFAULT_VALUE);
        String cassandraPassword = ConfProperties.getString(Constants.CASSANDRA_SERVER_AUTH_PROPERTY_NAME,
                Constants.CASSANDRA_SERVER_AUTH_DEFAULT_VALUE);
        String cassandraConnectionpoolSize = ConfProperties.getString(Constants.CASSANDRA_SERVER_CONNECTIONPOOL_SIZE,
                Constants.CASSANDRA_SERVER_CONNECTIONPOOL_SIZE_DEFAULT);
        String cassandraSslEnabled = ConfProperties.getString(Constants.CASSANDRA_SSL_ENABLED,
                Constants.CASSANDRA_SSL_ENABLED_DEFAULT);
        String cassandraReconnectDelay = ConfProperties.getString(Constants.CASSANDRA_RECONNECT_DELAY_MS,
                Constants.CASSANDRA_RECONNECT_DELAY_MS_DEFAULT);
        String cassandraReadTimeout = ConfProperties.getString(Constants.CASSANDRA_READ_TIMEOUT_MS,
                Constants.CASSANDRA_READ_TIMEOUT_MS_DEFAULT);

        String[] list = cassandraIpPortList.trim().split(",");

        List<InetSocketAddress> nodes = new ArrayList<InetSocketAddress>();

        for (String nodeIpPort : list) {

            String[] ipPort = nodeIpPort.trim().split(":", 3);
            String cassandraIp = ipPort[0];
            int cassandraPort = (ipPort.length > 1) ? Integer.parseInt(ipPort[1]) : Constants.CASSANDRA_SERVER_DEFAULT_PORT;
            InetSocketAddress node = new InetSocketAddress(cassandraIp, cassandraPort);

            nodes.add(node);
        }

        Map<String,Object> result = new HashMap<>();
        result.put(nodesKey, nodes);
        result.put(keyspaceKey, cassandraKeyspaceName);
        result.put(usernameKey, cassandraUsername);
        result.put(authKey,cassandraPassword);
        result.put(connectionpoolKey, cassandraConnectionpoolSize);
        result.put(sslEnabled, cassandraSslEnabled);
        result.put(reconnectDelay, cassandraReconnectDelay);
        result.put(readTimeout, cassandraReadTimeout);
        return result;

    }
}
