package com.heal.dashboard.service.util;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@Slf4j
@Component
public class JsonFileParser {

    @Autowired
    ResourceLoader resourceLoader;

    private volatile Map<String, Object> keycloakSSOConfigurations = null;
    private volatile Map<String, String> headerConfigurations = null;

    @Value("${ds.filename.headers.properties:headers_details.json}")
    private String headerPropertiesFileName;
    @Value("${ds.filename.keycloak.details:keycloak_details.json}")
    private String keycloakSsoConfFileName;

    public HttpHeaders loadHeaderConfiguration() {
        try {
            InputStream keycloakConfStream = resourceLoader.getResource("classpath:" + headerPropertiesFileName).getInputStream();
            if (headerConfigurations == null)
                headerConfigurations = CommonUtils.getObjectMapperWithHtmlEncoder().readValue(keycloakConfStream, new TypeReference<Map<String, String>>() {
                });
        } catch (Exception e) {
            log.info("Reading/Loading {} failed {}", headerPropertiesFileName, e);
        }

        HttpHeaders responseHeaders = new HttpHeaders();

        if(headerConfigurations == null) {
            responseHeaders = new HttpHeaders();
            headerConfigurations.forEach(responseHeaders::add);
        }

        return responseHeaders;
    }

    public Map<String, Object> loadKeycloakSSOConfig() {
        try {
            InputStream keycloakConfStream = resourceLoader.getResource("classpath:" + keycloakSsoConfFileName).getInputStream();
            if (keycloakSSOConfigurations == null)
                keycloakSSOConfigurations = CommonUtils.getObjectMapperWithHtmlEncoder().readValue(keycloakConfStream, new TypeReference<Map<String, Object>>() {
                });
        } catch (Exception e) {
            log.info("Reading/Loading {} failed {}", keycloakSsoConfFileName, e);
        }
        return keycloakSSOConfigurations;
    }

    public Map<String, String> getHeaderConfigurations() {
        return headerConfigurations;
    }
}
