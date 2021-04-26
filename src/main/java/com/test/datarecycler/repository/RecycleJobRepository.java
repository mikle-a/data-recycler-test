package com.test.datarecycler.repository;

import com.test.datarecycler.entity.RecycleJob;
import com.test.datarecycler.exception.JobNotFoundException;
import com.test.datarecycler.exception.JobForTableAlreadyExistsException;

import java.time.Instant;

/**
 * Repository to maintain recycle jobs state
 */
public interface RecycleJobRepository {

    /**
     * Create new recycle job. Generates id automatically.
     *
     * @param tableName         table name to delete rows from
     * @param datetimeFieldName datetime field name to query old rows
     * @param olderThan         specifies how old records should be deleted
     * @param batchSize         data will be deleted by batches in the loop
     * @return newly created job
     */
    RecycleJob newRecycleJob(String tableName, String datetimeFieldName, Instant olderThan, int batchSize)
            throws JobForTableAlreadyExistsException;

    /**
     * Get job by id
     *
     * @param id existing job id
     * @return existing job
     * @throws JobNotFoundException when job was not found
     */
    RecycleJob getJobById(String id) throws JobNotFoundException;

    /**
     * Get job operational state
     *
     * @param id job id
     * @return job operational state
     * @throws JobNotFoundException when job is not found
     */
    RecycleJob.State getJobStateById(String id) throws JobNotFoundException;

    /**
     * Update job statues
     *
     * @param id        id of existing job
     * @param newStatus new job status
     * @throws JobNotFoundException when job is not found
     */
    void updateJobStatus(String id, RecycleJob.Status newStatus) throws JobNotFoundException;

    /**
     * Increment job deleted count
     *
     * @param id              id of existing job
     * @param addDeletedCount amount of deleted rows to add
     */
    void incrementJobDeletedCount(String id, int addDeletedCount);

    /**
     * Remove job from the repository. Job operational state will be removed after the configured delay, allowing
     * clients to get job state even after completion or cancellation.
     *
     * @param id id of existing job
     * @throws JobNotFoundException when job is not found
     */
    void removeJob(String id) throws JobNotFoundException;

}
