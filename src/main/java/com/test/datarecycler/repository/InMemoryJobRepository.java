package com.test.datarecycler.repository;

import com.test.datarecycler.entity.RecycleJob;
import com.test.datarecycler.exception.JobNotFoundException;
import com.test.datarecycler.exception.JobForTableAlreadyExistsException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Threadsafe in memory implementation of {@link RecycleJobRepository}.
 */
@Slf4j
public class InMemoryJobRepository implements RecycleJobRepository {

    private final Map<String, RecycleJob> jobsById;
    private final Map<String, RecycleJob> tableJobs;
    private final Map<RecycleJob, RecycleJob.State> jobState;
    private final Duration oldJobsRetentionPeriod;
    private final ScheduledExecutorService scheduler;

    /**
     * Create new instance
     * @param scheduler scheduler to execute removal tasks after the completeJobsRetentionPeriod
     * @param completeJobsRetentionPeriod period of time during which complete tasks won't be deleted, allowing clients
     *                                    to checks its state
     */
    public InMemoryJobRepository(@NonNull ScheduledExecutorService scheduler,
                                 @NonNull Duration completeJobsRetentionPeriod) {
        this.jobsById = new HashMap<>();
        this.tableJobs = new HashMap<>();
        this.jobState = new HashMap<>();
        this.scheduler = scheduler;
        this.oldJobsRetentionPeriod = completeJobsRetentionPeriod;
    }

    @Override
    public synchronized RecycleJob newRecycleJob(@NonNull String tableName,
                                                 @NonNull String datetimeFieldName,
                                                 @NonNull Instant olderThan,
                                                 int batchSize) {
        if (tableJobs.containsKey(tableName)) {
            throw new JobForTableAlreadyExistsException(tableJobs.get(tableName));
        }

        final RecycleJob newJob = new RecycleJob(
                UUID.randomUUID().toString(),
                tableName,
                datetimeFieldName,
                olderThan,
                batchSize);

        tableJobs.put(tableName, newJob);
        jobsById.put(newJob.getId(), newJob);
        jobState.put(newJob, new RecycleJob.State(RecycleJob.Status.PENDING, 0));

        return newJob;
    }

    @Override
    public synchronized RecycleJob getJobById(@NonNull String id) throws JobNotFoundException {
        return Optional.ofNullable(jobsById.get(id))
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    @Override
    public synchronized RecycleJob.State getJobStateById(@NonNull String id) throws JobNotFoundException {
        return Optional.ofNullable(jobState.get(getJobById(id)))
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    @Override
    public synchronized void updateJobStatus(@NonNull String id, @NonNull RecycleJob.Status newStatus)
            throws JobNotFoundException {
        final RecycleJob.State state = getJobStateById(id);
        if (state.getStatus().isCancellable()) {
            jobState.computeIfPresent(getJobById(id),
                    ($, oldState) -> new RecycleJob.State(newStatus, oldState.getDeletedCount()));
        }
    }

    @Override
    public synchronized void incrementJobDeletedCount(@NonNull String id, int addDeletedCount) {
        jobState.computeIfPresent(getJobById(id),
                ($, oldState) -> new RecycleJob.State(
                        oldState.getStatus(),
                        oldState.getDeletedCount() + addDeletedCount)
        );
    }

    @Override
    public synchronized void removeJob(@NonNull String id) throws JobNotFoundException {
        final RecycleJob recycleJob = getJobById(id);
        tableJobs.remove(recycleJob.getTableName(), recycleJob);
        scheduler.schedule(() -> {
            log.info("Remove old job '{}' from the registry", recycleJob.getId());
            jobsById.remove(recycleJob.getId(), recycleJob);
            jobState.remove(recycleJob);
        }, oldJobsRetentionPeriod.toMillis(), TimeUnit.MILLISECONDS);
    }
}
