package com.ethsimulator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnBean(DataSource.class)
@ComponentScan(basePackages = "com.ethsimulator.persistence")
public class PersistenceConfig {
}