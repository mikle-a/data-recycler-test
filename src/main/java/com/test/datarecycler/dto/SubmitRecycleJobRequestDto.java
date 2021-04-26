package com.test.datarecycler.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
public class SubmitRecycleJobRequestDto {
    @NotNull
    private String tableName;
    @NotNull
    private String datetimeFieldName;
    @NotNull
    private Instant olderThan;
}
