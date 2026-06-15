package com.ethsimulator.audit;

import java.util.List;

public final class AuditCsvExporter {

    private static final String HEADER = "token,tx_hash,log_index,from_address,to_address,amount,block_number,occurred_at";

    private AuditCsvExporter() {
    }

    public static String export(List<AuditEvent> events) {
        StringBuilder csv = new StringBuilder(HEADER);
        for (AuditEvent event : events) {
            csv.append('\n')
                    .append(escape(event.token())).append(',')
                    .append(escape(event.txHash())).append(',')
                    .append(event.logIndex()).append(',')
                    .append(escape(event.fromAddress())).append(',')
                    .append(escape(event.toAddress())).append(',')
                    .append(escape(event.amount())).append(',')
                    .append(event.blockNumber()).append(',')
                    .append(escape(event.occurredAt()));
        }
        return csv.toString();
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!mustQuote) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}