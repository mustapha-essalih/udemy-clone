package com.dev.lms.course_service.exception;

import com.dev.lms.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        return new ErrorResponse(404, "Not Found", ex.getMessage(),
                exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErrorResponse handleBusiness(BusinessException ex, ServerWebExchange exchange) {
        return new ErrorResponse(422, "Business Error", ex.getMessage(),
                exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return new ErrorResponse(400, "Validation Error", message,
                exchange.getRequest().getPath().value());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex, ServerWebExchange exchange) {
        return new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred",
                exchange.getRequest().getPath().value());
    }
}
