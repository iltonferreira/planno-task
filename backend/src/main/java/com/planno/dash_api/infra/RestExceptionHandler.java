package com.planno.dash_api.infra;

import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ForbiddenException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.infra.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(" | "));

        ErrorMessage errorResponse = new ErrorMessage(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorMessage errorResponse = new ErrorMessage(HttpStatus.BAD_REQUEST.value(), "Parametro obrigatorio ausente: " + ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    private ResponseEntity<ErrorMessage> handleNotFound(ResourceNotFoundException exception) {
        ErrorMessage errorResponse = new ErrorMessage(HttpStatus.NOT_FOUND.value(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    private ResponseEntity<ErrorMessage> handleBusiness(BusinessException exception) {
        ErrorMessage errorResponse = new ErrorMessage(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    private ResponseEntity<ErrorMessage> handleUnauthorized(UnauthorizedException exception) {
        ErrorMessage errorResponse = new ErrorMessage(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    private ResponseEntity<ErrorMessage> handleForbidden(ForbiddenException exception) {
        ErrorMessage errorResponse = new ErrorMessage(HttpStatus.FORBIDDEN.value(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    private ResponseEntity<ErrorMessage> handleConflict(DataIntegrityViolationException exception) {
        ErrorMessage errorResponse = new ErrorMessage(
                HttpStatus.CONFLICT.value(),
                "Conflito de dados: este registro ja existe no sistema."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    private ResponseEntity<ErrorMessage> runtimeHandler(RuntimeException exception) {
        LOGGER.error("Erro interno inesperado", exception);
        ErrorMessage errorResponse = new ErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno inesperado."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
