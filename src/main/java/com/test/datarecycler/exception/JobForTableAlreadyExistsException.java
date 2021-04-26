package com.test.datarecycler.exception;

import com.test.datarecycler.entity.RecycleJob;
import lombok.Getter;

@Getter
public class JobForTableAlreadyExistsException extends RuntimeException {
    private final RecycleJob recycleJob;

    public JobForTableAlreadyExistsException(RecycleJob existingJob) {
        super(String.format("Job for table '%s' already exists: %s", existingJob.getTableName(), existingJob.getId()));
        this.recycleJob = existingJob;
    }
}
