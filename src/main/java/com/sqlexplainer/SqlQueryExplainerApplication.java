package com.sqlexplainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SqlQueryExplainerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlQueryExplainerApplication.class, args);
    }
}
