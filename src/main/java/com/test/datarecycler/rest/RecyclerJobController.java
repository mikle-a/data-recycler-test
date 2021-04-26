package com.test.datarecycler.rest;

import com.test.datarecycler.dto.RecycleJobStateDto;
import com.test.datarecycler.dto.SubmitRecycleJobRequestDto;
import com.test.datarecycler.dto.SubmitRecycleJobResponseDto;
import com.test.datarecycler.entity.RecycleJob;
import com.test.datarecycler.service.RecycleJobService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1.0/data/recycler/job")
public class RecyclerJobController {

    private final RecycleJobService recycleJobService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmitRecycleJobResponseDto submitRecycleJob(@Valid @RequestBody SubmitRecycleJobRequestDto request) {
        final RecycleJob createdJob = recycleJobService
                .submitRecycleJob(request.getTableName(), request.getDatetimeFieldName(), request.getOlderThan());
        return new SubmitRecycleJobResponseDto(createdJob.getId());
    }

    @GetMapping("/{jobId}")
    public RecycleJobStateDto getRecycleJobStatus(@PathVariable String jobId) {
        return convertToDto(recycleJobService.getRecycleJobState(jobId));
    }

    @DeleteMapping("/{jobId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RecycleJobStateDto cancelRecycleJob(@PathVariable String jobId) {
        return convertToDto(recycleJobService.cancelRecycleJob(jobId));
    }

    private RecycleJobStateDto convertToDto(RecycleJob.State recycleJobState) {
        return new RecycleJobStateDto(recycleJobState.getStatus(), recycleJobState.getDeletedCount());
    }

}
