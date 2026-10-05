package com.hs.hstesis.shared.interfaces.rest.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.hs.hstesis.iam.domain.exceptions.*;
import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.repo.domain.exceptions.*;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            GradingPeriodInPastException.class,
            InvalidGradingPeriodYearException.class,
            GradingPeriodDurationTooShortException.class,
            InvalidRoleException.class,
            UploadedFileIsEmptyException.class
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
        logger.warn("Request body is not readable");
        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            Class<?> targetType = invalidFormatException.getTargetType();

            var errorResponse = new ApiErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Type Mismatch",
                    String.format("Invalid value for expected type '%s'", targetType.getSimpleName())
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Do not include the rejected value or the conversion exception in the response or logs.
        logger.warn("Invalid request parameter type");
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Parameter",
                "A request parameter has an invalid type."
        ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMethod(HttpRequestMethodNotSupportedException ex) {
        logger.warn("Unsupported HTTP request method");
        return new ResponseEntity<>(new ApiErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                "This HTTP method is not supported for this endpoint."
        ), ex.getHeaders(), HttpStatus.METHOD_NOT_ALLOWED);
    }

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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(AccessDeniedException ex, jakarta.servlet.http.HttpServletRequest request) {
        logger.error("Forbidden: {}", ex.getMessage());
        
        String message = "You do not have permission to access this resource.";
        
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().endsWith("/api/v1/users")) {
            message = "Solo el administrador del sistema puede registrar nuevos usuarios.";
        }
        
        var errorResponse = new ApiErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

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
            CannotRemoveLastAdminException.class,
            UnauthorizedRoleAssignmentException.class,
            IncompatibleRoleException.class,
            RoleInUseException.class,
            CoordinatorNotAssignedToAnyAreaException.class,
            CoordinatorDoesNotOwnCourseException.class,
            TopicDoesNotBelongToCourseException.class,
            DocumentWithoutTargetsException.class,
            DocumentAlreadyExistsException.class,
            TopicAlreadyExistsException.class,
            InvalidDocumentStatusTransitionException.class,
            DocumentChunksRequiredException.class,
            IllegalStateException.class
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

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleMissingRoute(Exception ex) {
        // Do not expose or log user-controlled paths, query strings or resource locations.
        logger.debug("Requested route or static resource was not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(),
                "The requested resource was not found."
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInternalError(Exception ex) {
        // Spring MVC request errors (missing parameters, media negotiation, validation)
        // already carry a 4xx status. Do not turn these into false server failures.
        if (ex instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            var status = error.getStatusCode();
            var knownStatus = HttpStatus.resolve(status.value());
            logger.warn("Rejected HTTP request with status {}", status.value());
            return new ResponseEntity<>(new ApiErrorResponse(status.value(),
                    knownStatus == null ? "Client Error" : knownStatus.getReasonPhrase(),
                    "The request could not be processed."), error.getHeaders(), status);
        }
        logger.error("Internal Server Error: ", ex);
        var errorResponse = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please contact support."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DocumentDeduplicationStateException.class)
    public ResponseEntity<ApiErrorResponse> handleDeduplicationState(DocumentDeduplicationStateException ex) {
        logger.error("Deduplication state error", ex);
        var errorResponse = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "A deduplication inconsistency occurred. Please try again or contact support if the issue persists."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FileStorageUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleFileStorageUnavailable(FileStorageUnavailableException ex) {
        logger.error("File storage unavailable: {}", ex.getMessage(), ex);
        var errorResponse = new ApiErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "File storage is unavailable. Please try again or contact support."
        );
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "30")
                .body(errorResponse);
    }

    @ExceptionHandler(MessageBrokerUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageBrokerUnavailable(MessageBrokerUnavailableException ex) {
        logger.error("Message broker unavailable. operation={}", ex.getMessage(), ex);

        var errorResponse = new ApiErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Message broker is unavailable. Please try again shortly."
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "15")
                .body(errorResponse);
    }
}
