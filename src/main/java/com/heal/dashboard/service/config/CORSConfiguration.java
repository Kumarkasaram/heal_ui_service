//package com.heal.dashboard.service.config;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import com.heal.dashboard.service.util.JsonFileParser;
//
///**
// * @author Sourav Suman - 08-11-2021
// */
//
//@Configuration
//@EnableWebSecurity
//public class CORSConfiguration extends WebSecurityConfigurerAdapter {
//
//    @Autowired
//    JsonFileParser headersParser;
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.headers().xssProtection();
//        http.cors().configurationSource(corsConfigurationSource())
//                .and()
//                .csrf().disable();
//    }
//
//    @Bean
//    CorsConfigurationSource corsConfigurationSource() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration configuration = new CorsConfiguration();
//       // HttpHeaders httpHeaders = headersParser.loadCrossOriginHeaderConfiguration();
//      //  List<HttpMethod> methodList = httpHeaders.getAccessControlAllowMethods();
//        configuration.setAllowedOrigins(Arrays.asList(httpHeaders.getAccessControlAllowOrigin()));
//        configuration.setAllowedHeaders(httpHeaders.getAccessControlAllowHeaders());
//        configuration.setAllowedMethods(methodList.stream().map(method -> method.name()).collect(Collectors.toList()));
//        configuration.setAllowCredentials(httpHeaders.getAccessControlAllowCredentials());
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//}
