package com.test.datarecycler.repository;

import com.test.datarecycler.entity.RecycleJob;
import com.test.datarecycler.exception.JobNotFoundException;
import com.test.datarecycler.exception.JobForTableAlreadyExistsException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class InMemoryJobRepositoryTest {

    public static final Duration COMPLETE_JOBS_RETENTION_PERIOD = Duration.ofSeconds(60);
    @Mock
    private ScheduledExecutorService scheduledExecutorServiceMock;

    private InMemoryJobRepository jobRepository;

    @BeforeEach
    public void init() {
        jobRepository = new InMemoryJobRepository(scheduledExecutorServiceMock, COMPLETE_JOBS_RETENTION_PERIOD);
    }

    @Test
    public void testCreateExistingTable(){
        //given there is job for the table already
        final String tableName = "table";
        final String fieldName = "field";
        final Instant instant = Instant.now();
        final int batchSize = 1000;

        final RecycleJob job = jobRepository.newRecycleJob(tableName, fieldName, instant, batchSize);

        //when another job for the same table is requested, then exception is thrown
        Assertions.assertThrows(JobForTableAlreadyExistsException.class,
                () -> jobRepository.newRecycleJob(tableName, fieldName, instant, batchSize));
    }

    @Test
    public void testGetByJobId() {
        //given job params
        final String tableName = "table";
        final String fieldName = "field";
        final Instant instant = Instant.now();
        final int batchSize = 1000;

        //when job is created
        final RecycleJob newJob = jobRepository.newRecycleJob(tableName, fieldName, instant, batchSize);

        //then it could be retrieved using getById and all fields match
        final RecycleJob job = jobRepository.getJobById(newJob.getId());
        Assertions.assertEquals(newJob, job);
        Assertions.assertEquals(tableName, job.getTableName());
        Assertions.assertEquals(fieldName, job.getFieldName());
        Assertions.assertEquals(batchSize, job.getBatchSize());
        Assertions.assertEquals(instant, job.getOlderThan());
    }

    @Test
    public void testGetByUnknownJobId() {
        //given
        final String unknownId = "123";

        //when repository is requested for unknown job id, then exception is thrown
        Assertions.assertThrows(JobNotFoundException.class, () -> jobRepository.getJobById("123"));
    }

    @Test
    public void testGetJobState() {
        //given just created job
        final RecycleJob createdJob = jobRepository.newRecycleJob("table", "field", Instant.now(), 1000);

        //when job state is requested
        final RecycleJob.State state = jobRepository.getJobStateById(createdJob.getId());

        //then initial state is returned
        Assertions.assertNotNull(state);
        Assertions.assertEquals(RecycleJob.Status.PENDING, state.getStatus());
        Assertions.assertEquals(0, state.getDeletedCount());
    }

    @Test
    public void testGetStateForUnknownJob() {
        //given unknown job id
        final String unknownJobId = "123";

        //when repository is requested for unknown job, then exception is thrown
        Assertions.assertThrows(JobNotFoundException.class, () -> jobRepository.getJobStateById(unknownJobId));
    }

    @Test
    public void testUpdateJobStatus() {
        //given just created job
        final RecycleJob job = jobRepository.newRecycleJob("table", "field", Instant.now(), 1000);

        //when status update is requested
        jobRepository.updateJobStatus(job.getId(), RecycleJob.Status.RUNNING);

        //then new status is applied actually
        final RecycleJob.State state = jobRepository.getJobStateById(job.getId());
        Assertions.assertEquals(RecycleJob.Status.RUNNING, state.getStatus());
    }

    @Test
    public void testUpdateUnknownJobStatus() {
        //given unknown job id
        final String unknownJobId = "123";

        //when repository is requested for unknown job, then exception is thrown
        Assertions.assertThrows(JobNotFoundException.class,
                () -> jobRepository.updateJobStatus(unknownJobId, RecycleJob.Status.RUNNING));
    }

    @Test
    public void testIncrementDeletedCount() {
        //given just created job
        final RecycleJob job = jobRepository.newRecycleJob("table", "field", Instant.now(), 1000);

        //when increment deleted count is requested
        jobRepository.incrementJobDeletedCount(job.getId(), 100500);

        //then counter is incremented actually
        final RecycleJob.State state = jobRepository.getJobStateById(job.getId());
        Assertions.assertEquals(100500, state.getDeletedCount());

        //when counter is request to be incremented again
        jobRepository.incrementJobDeletedCount(job.getId(), 500);

        //then counter is incremented actually and state remains untouched
        final RecycleJob.State state2 = jobRepository.getJobStateById(job.getId());
        Assertions.assertEquals(101000, state2.getDeletedCount());
        Assertions.assertEquals(state.getStatus(), state2.getStatus());
    }

    @Test
    public void testIncrementDeletedCountForUnknownJob() {
        //given unknown job id
        final String unknownJobId = "123";

        //when repository is requested for unknown job, then exception is thrown
        Assertions.assertThrows(JobNotFoundException.class,
                () -> jobRepository.incrementJobDeletedCount(unknownJobId, 100500));
    }

    @Test
    public void testRemoveJob() {
        //given just created job
        final String tableName = "table";
        final RecycleJob job = jobRepository.newRecycleJob(tableName, "field", Instant.now(), 1000);
        final ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);

        //when job removal is request
        jobRepository.removeJob(job.getId());

        //then job won't interfere to create new job for this table
        jobRepository.newRecycleJob(tableName, "field", Instant.now(), 1000);

        //but still job could retrieved by id
        Assertions.assertSame(job, jobRepository.getJobById(job.getId()));
        Assertions.assertNotNull(jobRepository.getJobStateById(job.getId()));

        //and delayed task to finally remove job was submitted
        Mockito.verify(scheduledExecutorServiceMock).schedule(captor.capture(),
                Mockito.eq(COMPLETE_JOBS_RETENTION_PERIOD.toMillis()), Mockito.eq(TimeUnit.MILLISECONDS));
        final Runnable delayedTask = captor.getValue();

        //and job is finally removed after delayed task completion
        delayedTask.run();
        Assertions.assertThrows(JobNotFoundException.class, () -> jobRepository.getJobById(tableName));
        Assertions.assertThrows(JobNotFoundException.class, () -> jobRepository.getJobStateById(tableName));
    }

    @Test
    public void testRemoveUnknownJob() {
        //given unknown job id
        final String unknownJobId = "123";

        //when repository is requested for unknown job, then exception is thrown
        Assertions.assertThrows(JobNotFoundException.class, () -> jobRepository.removeJob(unknownJobId));
    }

}