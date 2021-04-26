package com.test.datarecycler.service;

import com.test.datarecycler.entity.RecycleJob;
import com.test.datarecycler.exception.JobForTableAlreadyExistsException;
import com.test.datarecycler.exec.RecycleJobExecutor;
import com.test.datarecycler.repository.RecycleJobRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class RecycleJobServiceTest {

    public static final int BATCH_SIZE = 1000;
    @Mock
    private RecycleJobRepository recycleJobRepositoryMock;

    @Mock
    private RecycleJobExecutor recycleJobExecutorMock;

    private RecycleJobService recycleJobService;

    @BeforeEach
    public void init() {
        recycleJobService = new RecycleJobService(recycleJobRepositoryMock, recycleJobExecutorMock, BATCH_SIZE);
    }

    @Test
    public void testSubmitJob() {
        //given there is not job for the table
        final String tableName = "table";
        final String field = "field";
        final Instant olderThan = Instant.now();

        final RecycleJob jobMock = Mockito.mock(RecycleJob.class);

        Mockito.when(recycleJobRepositoryMock.newRecycleJob(tableName, field, olderThan, BATCH_SIZE))
                .thenReturn(jobMock);

        //when client attempts to submit the job
        final RecycleJob recycleJob = recycleJobService.submitRecycleJob(tableName, field, olderThan);

        //then new job is created in the repository and is submitted for execution
        Assertions.assertSame(jobMock, recycleJob);
        Mockito.verify(recycleJobExecutorMock, Mockito.times(1)).submitJob(jobMock);
    }

    @Test
    public void testSubmitAlreadyExistingTableJob() {
        //given there is job for the table
        final String tableName = "table";
        final String field = "field";
        final Instant olderThan = Instant.now();

        final RecycleJob existingJob = Mockito.mock(RecycleJob.class);
        Mockito.doThrow(new JobForTableAlreadyExistsException(existingJob))
                .when(recycleJobRepositoryMock).newRecycleJob(tableName, field, olderThan, BATCH_SIZE);

        //when client attempts to submit another job for the same table
        try {
            recycleJobService.submitRecycleJob(tableName, "field", olderThan);
        } catch (JobForTableAlreadyExistsException e) {
            //then exception is thrown
            Assertions.assertSame(existingJob, e.getRecycleJob());
        }
    }

    @Test
    public void testGetJobState() {
        //given there is previously created job
        final String jobId = "123";
        final RecycleJob.State existingJobState = Mockito.mock(RecycleJob.State.class);
        Mockito.when(recycleJobRepositoryMock.getJobStateById(jobId))
                .thenReturn(existingJobState);

        //when client attempts to get job state
        final RecycleJob.State recycleJobState = recycleJobService.getRecycleJobState(jobId);

        //then expected job state is returned
        Assertions.assertSame(existingJobState, recycleJobState);
    }

    @Test
    public void testCancelJob() {
        //given there is previously created job
        final String jobId = "123";
        final RecycleJob.State existingJobState = Mockito.mock(RecycleJob.State.class);
        Mockito.when(recycleJobRepositoryMock.getJobStateById(jobId))
                .thenReturn(existingJobState);

        //when client attempts to cancel the
        final RecycleJob.State recycleJobState = recycleJobService.cancelRecycleJob(jobId);

        //then service updates job status and returns last state
        Mockito.verify(recycleJobRepositoryMock, Mockito.times(1))
                .updateJobStatus(jobId, RecycleJob.Status.CANCELLED);
        Assertions.assertSame(existingJobState, recycleJobState);
    }


}