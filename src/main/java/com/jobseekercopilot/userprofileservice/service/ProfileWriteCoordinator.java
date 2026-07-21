package com.jobseekercopilot.userprofileservice.service;

import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ProfileWriteCoordinator {

    private static final int LOCK_STRIPES = 256;

    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ReentrantLock[] localLocks = new ReentrantLock[LOCK_STRIPES];
    private volatile Boolean postgres;

    public ProfileWriteCoordinator(
            TransactionTemplate transactionTemplate,
            JdbcTemplate jdbcTemplate) {
        this.transactionTemplate = transactionTemplate;
        this.jdbcTemplate = jdbcTemplate;
        for (int index = 0; index < localLocks.length; index++) {
            localLocks[index] = new ReentrantLock();
        }
    }

    public <T> T execute(String userId, Supplier<T> write) {
        ReentrantLock localLock = localLocks[Math.floorMod(userId.hashCode(), localLocks.length)];
        localLock.lock();
        try {
            return transactionTemplate.execute(status -> {
                if (isPostgres()) {
                    jdbcTemplate.query(
                            "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                            resultSet -> null,
                            userId);
                }
                return write.get();
            });
        } finally {
            localLock.unlock();
        }
    }

    private boolean isPostgres() {
        Boolean cached = postgres;
        if (cached != null) {
            return cached;
        }
        cached = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> connection.getMetaData()
                .getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql"));
        postgres = cached;
        return cached;
    }
}
