package com.ethsimulator.persistence;

import java.sql.Timestamp;
import java.time.Instant;

final class JdbcBindings {

    private JdbcBindings() {
    }

    static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}