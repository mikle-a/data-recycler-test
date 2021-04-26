package com.test.datarecycler.dto;

import com.test.datarecycler.entity.RecycleJob;
import lombok.Value;

@Value
public class RecycleJobStateDto {
    private RecycleJob.Status state;
    private int deletedCount;
}
