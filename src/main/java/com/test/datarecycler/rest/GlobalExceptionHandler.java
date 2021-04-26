package com.test.datarecycler.rest;

import com.test.datarecycler.dto.ErrorResponseDto;
import com.test.datarecycler.exception.JobNotFoundException;
import com.test.datarecycler.exception.JobForTableAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(JobForTableAlreadyExistsException.class)
    public ErrorResponseDto handleJobAlreadyExists(JobForTableAlreadyExistsException e) {
        log.warn(e.getMessage());
        return new ErrorResponseDto(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(JobNotFoundException.class)
    public ErrorResponseDto handleJobNotFound(JobNotFoundException e) {
        log.warn(e.getMessage());
        return new ErrorResponseDto(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponseDto handleValidationError(MethodArgumentNotValidException e) {
        final List<String> messages = e.getBindingResult().getAllErrors().stream().map((error) -> {
            final String fieldName = ((FieldError) error).getField();
            final String errorType = error.getDefaultMessage();
            return String.format("field '%s' %s", fieldName, errorType);
        }).collect(Collectors.toList());

        return new ErrorResponseDto(HttpStatus.BAD_REQUEST.value(), messages);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponseDto handleUnexpectedErrors(Exception e) {
        log.error("Unexpected error: {}", e.toString(), e);
        return new ErrorResponseDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

}
