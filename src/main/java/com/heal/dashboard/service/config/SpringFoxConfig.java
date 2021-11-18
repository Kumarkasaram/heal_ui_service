package com.heal.dashboard.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.BasicAuth;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@Configuration
public class SpringFoxConfig implements WebMvcConfigurer {

    @Value("{server.servlet.context-path:/v2.0/ui")
    private String baseUri;

        @Bean
        public Docket productApi() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.any())
                    .paths(PathSelectors.any())
                    .build();

        }
        @SuppressWarnings("unused")
		private ApiInfo metaData() {
            return new ApiInfo(
                    "Spring Boot REST API",
                    "Spring Boot REST API for Account services",
                    "1.0",
                    "Terms of service",
                    "testing",
                    "Apache License Version 2.0",
                    "https://www.apache.org/licenses/LICENSE-2.0");
        }

        @Bean
        @ConditionalOnMissingBean(ApiInfo.class)
        public ApiInfo apiInfo() {
            return ApiInfo.DEFAULT;
        }


        @Bean
        @ConditionalOnMissingBean(SecurityScheme.class)
        public SecurityScheme securityScheme() {
            return new BasicAuth("basicAuth");
        }
        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            registry.addRedirectViewController(baseUri, "/");
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler(baseUri + "/**").addResourceLocations("classpath:/META-INF/resources/");
        }
}
