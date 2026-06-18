package com.ethsimulator.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${DATABASE_URL:}')")
@ImportAutoConfiguration({DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(Environment environment) {
        String url = environment.getProperty("DATABASE_URL");
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("DATABASE_URL must be set when DataSourceConfig is active");
        }
        HikariDataSource dataSource = new HikariDataSource();
        String jdbcUrl = url.trim();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setMaximumPoolSize(5);
        dataSource.setConnectionTimeout(5_000);
        // Direct/session pooler URLs are preferred. For Supabase transaction pooler (6543),
        // append prepareThreshold=0 only when prepared statements fail behind pgbouncer.
        if (jdbcUrl.contains("pgbouncer=true") && !jdbcUrl.contains("prepareThreshold=")) {
            String separator = jdbcUrl.contains("?") ? "&" : "?";
            dataSource.setJdbcUrl(jdbcUrl + separator + "prepareThreshold=0");
        }
        return dataSource;
    }
}