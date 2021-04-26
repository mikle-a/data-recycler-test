package com.test.datarecycler.exec;

import com.test.datarecycler.db.Database;
import com.test.datarecycler.entity.RecycleJob;
import com.test.datarecycler.repository.RecycleJobRepository;
import org.apache.tomcat.util.threads.InlineExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class LocalRecycleJobExecutorTest {

    @Mock
    private Database databaseMock;

    @Mock
    private RecycleJobRepository recycleJobRepositoryMock;

    private LocalRecycleJobExecutor jobExecutor;

    @BeforeEach
    public void init() {
        jobExecutor = new LocalRecycleJobExecutor(databaseMock,
                recycleJobRepositoryMock, new InlineExecutorService());
    }

    @Test
    public void testJobExecution() {
        //given some records in the table
        final RecycleJob job = new RecycleJob("123", "table", "field", Instant.now(), 1000);
        Mockito.when(recycleJobRepositoryMock.getJobStateById(job.getId()))
                .thenReturn(new RecycleJob.State(RecycleJob.Status.PENDING, 0))
                .thenReturn(new RecycleJob.State(RecycleJob.Status.RUNNING, 1000));

        Mockito.when(databaseMock.deleteData(job.getTableName(), job.getFieldName(), job.getOlderThan(),
                job.getBatchSize())).thenReturn(1000).thenReturn(537);

        final InOrder inOrder = Mockito.inOrder(recycleJobRepositoryMock, databaseMock);

        //when job is submitted
        jobExecutor.submitJob(job);

        //and on the first iteration status is toggled to RUNNING and 1000 records are deleted
        inOrder.verify(recycleJobRepositoryMock).getJobStateById(job.getId());
        inOrder.verify(recycleJobRepositoryMock).updateJobStatus(job.getId(), RecycleJob.Status.RUNNING);
        inOrder.verify(databaseMock).deleteData(job.getTableName(), job.getFieldName(), job.getOlderThan(), job.getBatchSize());
        inOrder.verify(recycleJobRepositoryMock).incrementJobDeletedCount(job.getId(), 1000);

        //and on the next iteration 537 records are deleted and job status is toggled to FINISHED
        inOrder.verify(recycleJobRepositoryMock).getJobStateById(job.getId());
        inOrder.verify(databaseMock).deleteData(job.getTableName(), job.getFieldName(), job.getOlderThan(), job.getBatchSize());
        inOrder.verify(recycleJobRepositoryMock).incrementJobDeletedCount(job.getId(), 537);
        inOrder.verify(recycleJobRepositoryMock).updateJobStatus(job.getId(), RecycleJob.Status.FINISHED);
    }

    @Test
    public void testCancelledJobExecution() {
        //given job will be cancelled in the middle of the process
        final RecycleJob job = new RecycleJob("123", "table", "field", Instant.now(), 1000);
        Mockito.when(recycleJobRepositoryMock.getJobStateById(job.getId()))
                .thenReturn(new RecycleJob.State(RecycleJob.Status.CANCELLED, 0));

        final InOrder inOrder = Mockito.inOrder(recycleJobRepositoryMock, databaseMock);

        //when job is submitted
        jobExecutor.submitJob(job);

        //then current state is checked and job is removed
        inOrder.verify(recycleJobRepositoryMock).getJobStateById(job.getId());
        inOrder.verify(recycleJobRepositoryMock).removeJob(job.getId());
        inOrder.verify(recycleJobRepositoryMock, Mockito.never()).updateJobStatus(Mockito.eq(job.getId()), Mockito.any());
        inOrder.verify(databaseMock, Mockito.never()).deleteData(job.getTableName(), job.getFieldName(), job.getOlderThan(), job.getBatchSize());
    }

    @Test
    public void testCancelJobInProgress() {
        //given job will be cancelled in the middle of the process
        final RecycleJob job = new RecycleJob("123", "table", "field", Instant.now(), 1000);
        Mockito.when(recycleJobRepositoryMock.getJobStateById(job.getId()))
                .thenReturn(new RecycleJob.State(RecycleJob.Status.PENDING, 0))
                .thenReturn(new RecycleJob.State(RecycleJob.Status.CANCELLED, 1000));

        Mockito.when(databaseMock.deleteData(job.getTableName(), job.getFieldName(), job.getOlderThan(),
                job.getBatchSize())).thenReturn(1000);

        final InOrder inOrder = Mockito.inOrder(recycleJobRepositoryMock, databaseMock);

        //when job is submitted
        jobExecutor.submitJob(job);

        //and on the first iteration status is toggled to RUNNING and 1000 records are deleted
        inOrder.verify(recycleJobRepositoryMock).getJobStateById(job.getId());
        inOrder.verify(recycleJobRepositoryMock).updateJobStatus(job.getId(), RecycleJob.Status.RUNNING);
        inOrder.verify(databaseMock).deleteData(job.getTableName(), job.getFieldName(), job.getOlderThan(), job.getBatchSize());
        inOrder.verify(recycleJobRepositoryMock).incrementJobDeletedCount(job.getId(), 1000);

        //and on the next iteration job cancellation is detected, status is toggled to FINISHED and no more records are deleted
        inOrder.verify(recycleJobRepositoryMock).getJobStateById(job.getId());
        inOrder.verify(databaseMock, Mockito.never()).deleteData(job.getTableName(), job.getFieldName(), job.getOlderThan(), job.getBatchSize());
        inOrder.verify(recycleJobRepositoryMock, Mockito.never()).incrementJobDeletedCount(job.getId(), 537);
        inOrder.verify(recycleJobRepositoryMock, Mockito.never()).updateJobStatus(job.getId(), RecycleJob.Status.FINISHED);
    }



}