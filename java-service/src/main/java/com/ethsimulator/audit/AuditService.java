package com.ethsimulator.audit;

import com.ethsimulator.api.error.ApiException;
import com.ethsimulator.blockchain.TransferEventRecord;
import com.ethsimulator.config.TokenAllowlist;
import com.ethsimulator.util.EvmAddressValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AuditService {

    private static final String MASKED_AMOUNT = "***";
    private static final List<String> ASSUMPTIONS = List.of(
            "Transfer history is sourced from on-chain ERC-20 Transfer event logs.",
            "Only allowlisted Ethereum mainnet tokens are included: USDC, USDT, DAI, GHO.",
            "Audit results are cached in memory after the first fetch per address."
    );

    private final AuditCache auditCache;

    public AuditService(AuditCache auditCache) {
        this.auditCache = auditCache;
    }

    public AuditResponse audit(
            String address,
            String from,
            String to,
            String token,
            boolean hideValues
    ) {
        String normalizedAddress = EvmAddressValidator.requireValid(address);
        TokenAllowlist.TokenEntry tokenFilter = resolveTokenFilter(token);
        Instant fromInstant = parseInstant(from, "from");
        Instant toInstant = parseInstant(to, "to");

        List<AuditEvent> events = auditCache.eventsFor(normalizedAddress).stream()
                .filter(event -> matchesToken(event, tokenFilter))
                .filter(event -> matchesFrom(event, fromInstant))
                .filter(event -> matchesTo(event, toInstant))
                .sorted(Comparator.comparing(TransferEventRecord::occurredAt).reversed()
                        .thenComparing(TransferEventRecord::blockNumber, Comparator.reverseOrder())
                        .thenComparing(TransferEventRecord::logIndex, Comparator.reverseOrder()))
                .map(event -> toAuditEvent(event, hideValues))
                .toList();

        List<String> assumptions = new ArrayList<>(ASSUMPTIONS);
        assumptions.add("Event source: " + auditCache.source() + ".");

        return new AuditResponse(normalizedAddress, events, hideValues, List.copyOf(assumptions));
    }

    public String exportCsv(
            String address,
            String from,
            String to,
            String token,
            boolean hideValues
    ) {
        return AuditCsvExporter.export(audit(address, from, to, token, hideValues).events());
    }

    private TokenAllowlist.TokenEntry resolveTokenFilter(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return TokenAllowlist.bySymbol(token)
                .orElseThrow(() -> new ApiException(
                        "INVALID_TOKEN",
                        "Token is not on the allowlist: " + token.trim(),
                        HttpStatus.BAD_REQUEST
                ));
    }

    private Instant parseInstant(String value, String paramName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (Exception ex) {
            throw new ApiException(
                    "INVALID_AUDIT_FILTER",
                    "Invalid " + paramName + " timestamp; expected ISO-8601 instant",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private boolean matchesToken(TransferEventRecord event, TokenAllowlist.TokenEntry tokenFilter) {
        return tokenFilter == null || event.token().equals(tokenFilter.symbol());
    }

    private boolean matchesFrom(TransferEventRecord event, Instant from) {
        return from == null || !event.occurredAt().isBefore(from);
    }

    private boolean matchesTo(TransferEventRecord event, Instant to) {
        return to == null || !event.occurredAt().isAfter(to);
    }

    private AuditEvent toAuditEvent(TransferEventRecord event, boolean hideValues) {
        String amount = hideValues
                ? MASKED_AMOUNT
                : event.amount().stripTrailingZeros().toPlainString();
        return new AuditEvent(
                event.token(),
                event.txHash(),
                event.logIndex(),
                event.fromAddress(),
                event.toAddress(),
                amount,
                event.blockNumber(),
                event.occurredAt().toString()
        );
    }
}