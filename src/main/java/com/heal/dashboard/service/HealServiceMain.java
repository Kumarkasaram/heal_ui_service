
package com.heal.dashboard.service;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableTransactionManagement(proxyTargetClass = true)
@PropertySource(value="classpath:conf.properties")
@Slf4j
public class HealServiceMain {
    public static void main(String[] args) {
        log.info("Heal dashboard service main is starting.......");
       // System.setProperty("logging.config", "classpath:logback.xml");
        SpringApplication.run(HealServiceMain.class, args);
    }
}


