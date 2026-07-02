package com.ethsimulator.market;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ethsimulator.config.EthSimulatorProperties;
import com.ethsimulator.persistence.RateObservation;
import com.ethsimulator.persistence.RateReadModelRepository;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YieldServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-09T12:00:00Z");
    private static final Instant FRESH_OBSERVED = NOW.minusSeconds(600);
    private static final Instant STALE_OBSERVED = NOW.minusSeconds(7200);

    @Mock
    private RateReadModelRepository rateReadModelRepository;

    @Mock
    private ObjectProvider<RateReadModelRepository> rateReadModelProvider;

    private EthSimulatorProperties properties;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        properties = new EthSimulatorProperties();
        properties.setYieldStaleThresholdSeconds(3600);
        Logger logger = (Logger) LoggerFactory.getLogger(YieldService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.ALL);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(YieldService.class);
        logger.detachAppender(appender);
    }

    @Test
    void seedYieldsAreLabelledDegraded() {
        YieldService yieldService = serviceWithoutRepository();

        YieldSnapshotResponse response = yieldService.getYields("USDC");

        assertThat(response.asset()).isEqualTo("USDC");
        assertThat(response.dataMode()).isEqualTo("seed_fallback");
        assertThat(response.yields()).isNotEmpty();
        assertThat(response.yields().getFirst().source()).isEqualTo("seed");
        assertThat(response.yields().getFirst().degraded()).isTrue();
        assertThat(response.yields().getFirst().observedAt()).isEqualTo(Instant.EPOCH);
        assertThat(response.yields().getFirst().convention()).isEqualTo(RateConvention.APR_EFFECTIVE);
    }

    @Test
    void unknownAssetUsesStaticFallback() {
        YieldService yieldService = serviceWithoutRepository();

        YieldSnapshotResponse response = yieldService.getYields("UNKNOWN");

        assertThat(response.dataMode()).isEqualTo("seed_fallback");
        assertThat(response.yields()).hasSize(1);
        assertThat(response.yields().getFirst().source()).isEqualTo("static_fallback");
        assertThat(response.yields().getFirst().protocol()).isEqualTo("static_conservative");
        assertThat(response.yields().getFirst().degraded()).isTrue();
    }

    @Test
    void livePathMapsRepositoryRows() {
        when(rateReadModelProvider.getIfAvailable()).thenReturn(rateReadModelRepository);
        when(rateReadModelRepository.findLatestByProducts(anyList())).thenReturn(List.of(
                liveObservation("aave", "GHO", FRESH_OBSERVED, "0.045000000000000000", "APR_SIMPLE")
        ));
        YieldService yieldService = serviceWithRepository();

        YieldSnapshotResponse response = yieldService.getYields("USDC");

        assertThat(response.dataMode()).isEqualTo("live");
        assertThat(response.yields()).hasSize(1);
        YieldQuote quote = response.yields().getFirst();
        assertThat(quote.protocol()).isEqualTo("aave");
        assertThat(quote.apyPct()).isEqualByComparingTo(FinancialMath.bd("4.5"));
        assertThat(quote.convention()).isEqualTo(RateConvention.APR_SIMPLE);
        assertThat(quote.source()).isEqualTo("aave");
        assertThat(quote.observedAt()).isEqualTo(FRESH_OBSERVED);
        assertThat(quote.degraded()).isFalse();
    }

    @Test
    void staleLiveRowsFlipDegraded() {
        when(rateReadModelProvider.getIfAvailable()).thenReturn(rateReadModelRepository);
        when(rateReadModelRepository.findLatestByProducts(anyList())).thenReturn(List.of(
                liveObservation("sky", "sUSDS", STALE_OBSERVED, "0.050000000000000000", "APR_EFFECTIVE")
        ));
        YieldService yieldService = serviceWithRepository();

        YieldSnapshotResponse response = yieldService.getYields("DAI");

        assertThat(response.dataMode()).isEqualTo("live");
        assertThat(response.yields().getFirst().protocol()).isEqualTo("maker_dsr");
        assertThat(response.yields().getFirst().degraded()).isTrue();
    }

    @Test
    void emptyRepositoryFallsBackToSeed() {
        when(rateReadModelProvider.getIfAvailable()).thenReturn(rateReadModelRepository);
        when(rateReadModelRepository.findLatestByProducts(anyList())).thenReturn(List.of());
        YieldService yieldService = serviceWithRepository();

        YieldSnapshotResponse response = yieldService.getYields("USDC");

        assertThat(response.dataMode()).isEqualTo("seed_fallback");
        assertThat(response.yields().getFirst().source()).isEqualTo("seed");
        assertThat(response.yields().getFirst().degraded()).isTrue();
    }

    @Test
    void throwingRepositoryFallsBackWithoutPropagating() {
        when(rateReadModelProvider.getIfAvailable()).thenReturn(rateReadModelRepository);
        doThrow(new RuntimeException("db down")).when(rateReadModelRepository).findLatestByProducts(anyList());
        YieldService yieldService = serviceWithRepository();

        YieldSnapshotResponse response = yieldService.getYields("USDC");

        assertThat(response.dataMode()).isEqualTo("seed_fallback");
        assertThat(appender.list).anyMatch(event ->
                event.getLevel() == Level.WARN && event.getFormattedMessage().contains("Yield read model unavailable"));
    }

    private YieldService serviceWithoutRepository() {
        when(rateReadModelProvider.getIfAvailable()).thenReturn(null);
        return new YieldService(Clock.fixed(NOW, ZoneOffset.UTC), rateReadModelProvider, properties);
    }

    private YieldService serviceWithRepository() {
        return new YieldService(Clock.fixed(NOW, ZoneOffset.UTC), rateReadModelProvider, properties);
    }

    private static RateObservation liveObservation(
            String protocol,
            String product,
            Instant observedAt,
            String annualizedValue,
            String convention
    ) {
        return new RateObservation(
                UUID.randomUUID(),
                protocol,
                product,
                "borrow",
                FinancialMath.bd(annualizedValue),
                convention,
                "trailing_realized",
                "7d",
                "0xcontract",
                1L,
                21_000_125L,
                "0xblock",
                observedAt,
                observedAt,
                true,
                false,
                observedAt
        );
    }
}