package com.hs.hstesis.shared.interfaces.rest.advice;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.shared.interfaces.rest.resources.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- BAD REQUEST (400) ---
    @ExceptionHandler({
            DateInPastException.class,
            DateYearMismatchException.class,
            InvalidAreaNameException.class,
            InvalidCourseNameException.class
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
            AcademicLevelNotFoundException.class,
            AcademicYearNotFoundException.class,
            AreaNotFoundException.class,
            ClassroomNotFoundException.class,
            CourseNotFoundException.class,
            EnrollmentNotFoundException.class,
            GradingPeriodNotFoundException.class,
            NoAcademicLevelsFoundException.class,
            RoleNotFoundException.class,
            SectionNotFoundException.class,
            StudyPlanEntryNotFoundException.class,
            UserNotFoundException.class
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
            AcademicLevelNameAlreadyExistsException.class,
            AcademicLevelRelatedToSectionsException.class,
            AcademicYearAlreadyExistsException.class,
            AcademicYearCannotBeClosedException.class,
            AcademicYearIsActiveException.class,
            AreaAlreadyHasCoordinatorException.class,
            AreaNameAlreadyExistsException.class,
            BimesterAlreadyExistsInAcademicYearException.class,
            CannotDeleteActiveClassroomException.class,
            CannotDeleteHistoricalDataException.class,
            CourseNameAlreadyExistsInAreaException.class,
            CourseAlreadyInStudyPlanException.class,
            GradingPeriodOverlapException.class,
            IncompleteAcademicYearException.class,
            InvalidGradingPeriodDeleteException.class,
            NoActiveAcademicYearException.class,
            SectionNameAlreadyExistsInAcademicLevelException.class
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
}