package com.test.datarecycler.service;

import com.test.datarecycler.entity.RecycleJob;
import com.test.datarecycler.exception.JobNotFoundException;
import com.test.datarecycler.exception.JobForTableAlreadyExistsException;
import com.test.datarecycler.exec.RecycleJobExecutor;
import com.test.datarecycler.repository.RecycleJobRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * Service providing functionality to work with data recycle jobs.
 */
@Slf4j
public class RecycleJobService {

    private final RecycleJobRepository jobRepository;
    private final RecycleJobExecutor jobExecutor;
    private final int batchSize;

    /**
     * Construct new instance
     * @param jobRepository repository to maintain recycle jobs state
     * @param recycleJobExecutor recycle jobs execution strategy
     * @param batchSize batch size to delete records
     */
    public RecycleJobService(@NonNull RecycleJobRepository jobRepository,
                             @NonNull RecycleJobExecutor recycleJobExecutor,
                             int batchSize) {
        if (batchSize <= 0) throw new IllegalArgumentException("batch size must be positive");
        this.batchSize = batchSize;
        this.jobRepository = jobRepository;
        this.jobExecutor = recycleJobExecutor;
    }

    /**
     * Submit new data recycle job, which will executed asynchronously. Only one active job per table is allowed.
     * @param tableName table name where data should deleted from
     * @param olderThan  specifies how old data should be deleted
     * @return newly created recycle job
     * @throws JobForTableAlreadyExistsException when table job aready exists
     */
    public RecycleJob submitRecycleJob(@NonNull String tableName,
                                       @NonNull String datetimeFieldName,
                                       @NonNull Instant olderThan) throws JobForTableAlreadyExistsException {
        final RecycleJob newJob = jobRepository.newRecycleJob(tableName, datetimeFieldName, olderThan, batchSize);
        jobExecutor.submitJob(newJob);

        log.info("Created recycle job '{}' to delete all records older than {} from the table '{}'",
                newJob.getId(), tableName, olderThan);

        return newJob;
    }

    /**
     * Get recycle job operational state
     * @param jobId of existing job
     * @return current operational state
     * @throws JobNotFoundException when job is not found
     */
    public RecycleJob.State getRecycleJobState(@NonNull String jobId) throws JobNotFoundException {
        return jobRepository.getJobStateById(jobId);
    }

    /**
     * Cancel recycle job
     * @param jobId id of existing job
     * @return last operational state
     * @throws JobNotFoundException when job is not found
     */
    public RecycleJob.State cancelRecycleJob(@NonNull String jobId)  throws JobNotFoundException {
        log.info("Cancel job '{}'", jobId);
        jobRepository.updateJobStatus(jobId, RecycleJob.Status.CANCELLED);
        return jobRepository.getJobStateById(jobId);
    }


}
