package com.test.datarecycler.exception;

import lombok.Getter;

@Getter
public class JobNotFoundException extends RuntimeException {

    private final String jobId;

    public JobNotFoundException(String jobId) {
        super(String.format("Job with id '%s' not found", jobId));
        this.jobId = jobId;
    }
}
