package by.assxmblxr.userservice.exception;

import by.assxmblxr.userservice.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFoundException(NotFoundException e, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflictException(ConflictException e, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, e.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValidException(
          MethodArgumentNotValidException e, HttpServletRequest request
  ) {
    String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleHttpMessageNotReadableException(
          HttpMessageNotReadableException e, HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(
          MethodArgumentTypeMismatchException e, HttpServletRequest request
  ) {
    String expectedType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
    String message = "Parameter '" + e.getName() + "' must be of type " + expectedType
            + ", but got '" + e.getValue() + "'";
    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrityViolationException(
          DataIntegrityViolationException e, HttpServletRequest request
  ) {
    log.warn("Data integrity violation on {}", request.getRequestURI(), e);
    return buildResponse(HttpStatus.CONFLICT, "Data integrity violation", request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleException(Exception e, HttpServletRequest request) {
    if (e instanceof ErrorResponse springError) {
      HttpStatus status = HttpStatus.resolve(springError.getStatusCode().value());
      if (status != null) {
        return buildResponse(status, e.getMessage(), request);
      }
    }
    log.error("Unexpected error on {}", request.getRequestURI(), e);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
  }

  private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
    ApiError body = new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI()
    );
    return ResponseEntity.status(status).body(body);
  }
}
