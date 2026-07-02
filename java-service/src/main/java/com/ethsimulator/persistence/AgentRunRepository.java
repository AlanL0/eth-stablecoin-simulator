package com.ethsimulator.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;

@Repository
@ConditionalOnBean(DataSource.class)
public class AgentRunRepository {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final JdbcClient jdbcClient;

    public AgentRunRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insert(AgentRunRecord record) {
        jdbcClient.sql("""
                insert into agent_runs (
                  prompt_version, provider, model, latency_ms, fallback_used, fallback_reason,
                  estimated_cost_usd, token_usage, free_quota_used, structured_output
                ) values (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb)
                """)
                .param(record.promptVersion())
                .param(record.provider())
                .param(record.model())
                .param(record.latencyMs())
                .param(record.fallbackUsed())
                .param(record.fallbackReason())
                .param(record.estimatedCostUsd())
                .param(toJson(record.tokenUsage()))
                .param(false)
                .param(record.structuredOutput())
                .update();
    }

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public record AgentRunRecord(
            String promptVersion,
            String provider,
            String model,
            int latencyMs,
            boolean fallbackUsed,
            String fallbackReason,
            BigDecimal estimatedCostUsd,
            Object tokenUsage,
            String structuredOutput
    ) {
    }
}