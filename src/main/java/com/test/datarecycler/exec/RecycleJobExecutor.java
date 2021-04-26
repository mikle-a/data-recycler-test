package com.test.datarecycler.exec;

import com.test.datarecycler.entity.RecycleJob;

/**
 * Recycle job execution strategy. Implementations are implied to updated job state using
 * {@link com.test.datarecycler.repository.RecycleJobRepository}
 */
public interface RecycleJobExecutor {

    /**
     * Submit recycle job for execution
     * @param recycleJob to be executed
     */
    void submitJob(RecycleJob recycleJob);
}
