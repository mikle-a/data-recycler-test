package com.test.datarecycler.exec;

import com.test.datarecycler.db.Database;
import com.test.datarecycler.entity.RecycleJob;
import com.test.datarecycler.repository.RecycleJobRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

/**
 * Local recycle job executor based on {@link ExecutorService}.
 */
@Slf4j
@AllArgsConstructor
public class LocalRecycleJobExecutor implements RecycleJobExecutor {

    private final Database database;
    private final RecycleJobRepository jobRepository;
    private final ExecutorService executorService;

    @Override
    public void submitJob(RecycleJob recycleJob) {
        executorService.submit(() -> executeJob(recycleJob));
    }

    /**
     * Removes specified data in the loop, according to the specified batch size. On each iteration, ensures
     * that task has not been cancelled. Updates job status and deleted-count after each loop.
     *
     * @param job job to executed
     */
    private void executeJob(RecycleJob job) {
        log.info("Start executing job '{}' to delete records older than '{}' from the '{}' table",
                job.getId(), job.getOlderThan(), job.getTableName());
        try {
            while (true) {
                //check actual state on each iteration
                final RecycleJob.State jobCurrentState = jobRepository.getJobStateById(job.getId());
                if (jobCurrentState.getStatus() == RecycleJob.Status.CANCELLED) {
                    log.info("Job '{}' has been cancelled, stop execution. Deleted records in total: {}",
                            job.getId(), jobCurrentState.getDeletedCount());
                    jobRepository.removeJob(job.getId());
                    return;
                } else if (jobCurrentState.getStatus() == RecycleJob.Status.PENDING) {
                    jobRepository.updateJobStatus(job.getId(), RecycleJob.Status.RUNNING);
                }

                //try to delete batch
                log.info("Job '{}' attempts to delete {} records from the '{}' table",
                        job.getId(), job.getBatchSize(), job.getTableName());
                final int deletedCount = database
                        .deleteData(job.getTableName(), job.getFieldName(), job.getOlderThan(), job.getBatchSize());

                //handle batch delete result
                log.info("Job '{}' has deleted {} records from the table '{}'",
                        job.getId(), deletedCount, job.getTableName());
                jobRepository.incrementJobDeletedCount(job.getId(), deletedCount);

                if (deletedCount < job.getBatchSize()) {
                    log.info("Job '{}' no more records to remove, finish. Deleted records in total: {}",
                            job.getId(), jobCurrentState.getDeletedCount());
                    jobRepository.updateJobStatus(job.getId(), RecycleJob.Status.FINISHED);
                    jobRepository.removeJob(job.getId());
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error on job {} execution", job.getId());
            jobRepository.updateJobStatus(job.getId(), RecycleJob.Status.FAILED);
        }
    }
}
