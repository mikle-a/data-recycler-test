package com.test.datarecycler.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

/**
 * Data recycle job
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class RecycleJob {

    private final String id;
    private final String tableName;
    private final String fieldName;
    private final Instant olderThan;
    private final int batchSize;

    public enum Status {
        PENDING, RUNNING, FINISHED, CANCELLED, FAILED;
        public boolean isCancellable() {
            return this == PENDING || this == RUNNING;
        }
    }

    /**
     * Data recycle job operational state
     */
    @Getter
    @AllArgsConstructor
    public static class State {
        private Status status;
        private int deletedCount;
    }
}
