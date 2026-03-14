package com.hs.hstesis.shared.interfaces.rest.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.hs.hstesis.iam.domain.exceptions.*;
import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;
import com.hs.hstesis.shared.interfaces.rest.resources.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- BAD REQUEST (400) ---
    @ExceptionHandler({
            GradingPeriodInPastException.class,
            InvalidGradingPeriodYearException.class,
            GradingPeriodDurationTooShortException.class,
            InvalidRoleException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex) {
        logger.error("Bad Request: {}", ex.getMessage());
        var errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // --- UNAUTHORIZED (401) ---
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        logger.error("Unauthorized: {}", ex.getMessage());
        var errorResponse = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication is required to access this resource."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    // --- FORBIDDEN (403) ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        logger.error("Forbidden: {}", ex.getMessage());
        var errorResponse = new ApiErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "You do not have permission to access this resource."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // --- NOT FOUND (404) ---
    @ExceptionHandler({
            ResourceNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        logger.error("Not Found: {}", ex.getMessage());
        var errorResponse = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // --- CONFLICT (409) ---
    @ExceptionHandler({
            AreaRelatedToCoursesException.class,
            AcademicYearAlreadyExistsException.class,
            AcademicYearCannotBeClosedException.class,
            AcademicYearIsActiveException.class,
            AreaNameAlreadyExistsException.class,
            BimesterAlreadyExistsInAcademicYearException.class,
            CannotDeleteHistoricalDataException.class,
            CourseNameAlreadyException.class,
            CourseAlreadyInStudyPlanException.class,
            GradingPeriodOverlapException.class,
            IncompleteAcademicYearException.class,
            SectionNameAlreadyExistsInAcademicLevelException.class,
            UserAlreadyIsACoordinatorException.class,
            UserIsNotACoordinatorException.class,
            BimesterSequencePredecessorException.class,
            BimesterSequenceSuccessorException.class,
            NoAcademicYearReadyException.class,
            FinishedGradingPeriodModificationException.class,
            StartedGradingPeriodModificationException.class,
            ClassroomGenerationDeadlineExceededException.class,
            NoClassroomsDefinedException.class,
            InvalidUserRoleException.class,
            TeacherAlreadyAssignedException.class,
            CannotRemoveLastAdminException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException ex) {
        logger.error("Conflict: {}", ex.getMessage());
        var errorResponse = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // --- INTERNAL SERVER ERROR (500) ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInternalError(Exception ex) {
        logger.error("Internal Server Error: ", ex);
        var errorResponse = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please contact support."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Validation Error: {}", ex.getMessage());
        var errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Argument",
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.error("Not Readable: {}", ex.getMessage());
        Throwable mostSpecificCause = ex.getMostSpecificCause();

        if (mostSpecificCause instanceof IllegalArgumentException illegalArgumentEx) {
            return handleIllegalArgument(illegalArgumentEx);
        }

        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            String rejectedValue = invalidFormatException.getValue().toString();
            Class<?> targetType = invalidFormatException.getTargetType();

            var errorResponse = new ApiErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Type Mismatch",
                    String.format("Invalid value '%s' for expected type '%s'", rejectedValue, targetType.getSimpleName())
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        var errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON",
                "The request body contains invalid JSON syntax or structure."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}