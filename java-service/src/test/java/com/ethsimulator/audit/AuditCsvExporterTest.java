package com.ethsimulator.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditCsvExporterTest {

    @Test
    void escapesCommasQuotesAndNewlines() {
        String csv = AuditCsvExporter.export(List.of(
                new AuditEvent(
                        "USDC",
                        "0xhash",
                        1,
                        "0xfrom",
                        "0xto",
                        "1,000.50",
                        42L,
                        "2026-01-01T00:00:00Z"
                ),
                new AuditEvent(
                        "DAI",
                        "0xhash2",
                        0,
                        "0xfrom\"special",
                        "0xto",
                        "plain",
                        43L,
                        "2026-01-02\nline2"
                )
        ));

        assertThat(csv).contains("\"1,000.50\"");
        assertThat(csv).contains("\"0xfrom\"\"special\"");
        assertThat(csv).contains("\"2026-01-02\nline2\"");
    }
}