package com.heal.dashboard.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration
@EnableCassandraRepositories(basePackages = "com.heal.eventdetector.repo")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${spring.data.cassandra.contactpoints:localhost}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port:9142}")
    private int port;

    @Value("${spring.data.cassandra.keyspace:appsone}")
    private String keySpace;

    @Value("${spring.data.cassandra.basePackages:com.heal.eventdetector}")
    private String basePackages;

    @Value("${spring.data.cassandra.username}")
    private String username;

    @Value("${spring.data.cassandra.password}")
    private String password;

    @Value("${spring.data.cassandra.sslEnabled:true}")
    private boolean sslEnabled;

    @Override
    protected String getKeyspaceName() {
        return keySpace;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[]{basePackages};
    }

    @Bean
    public CassandraClusterFactoryBean cluster() {
        CassandraClusterFactoryBean cluster = super.cluster();

        cluster.setUsername(username);
        cluster.setPassword(password);
        cluster.setContactPoints(contactPoints);
        cluster.setPort(port);
        cluster.setSslEnabled(sslEnabled);
        cluster.setJmxReportingEnabled(false);
        try {
            cluster.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cluster;
    }

//    @Bean
//    public CassandraOperations template() throws Exception {
//        return new CassandraTemplate(session().getObject());
//    }

}
