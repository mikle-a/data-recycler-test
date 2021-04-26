package com.test.datarecycler.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {
    private int status;
    private List<String> messages;

    public ErrorResponseDto(int status, String message) {
        this(status, Collections.singletonList(message));
    }
}

