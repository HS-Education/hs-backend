package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.services.EnrollmentCommandService;
import com.hs.hstesis.learning.interfaces.rest.resources.*;
import com.hs.hstesis.learning.interfaces.rest.transform.*;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping(value = "/api/enrollments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Enrollments", description = "Endpoints for managing student enrollments and teacher assignments")
public class EnrollmentController {
    private final EnrollmentCommandService enrollmentCommandService;

    public EnrollmentController(EnrollmentCommandService enrollmentCommandService) {
        this.enrollmentCommandService = enrollmentCommandService;
    }

    @Operation(description = "Enrolls students to an academic level.")
    @PostMapping("/students/enroll")
    public ResponseEntity<MessageResource> enrollStudentsToAcademicLevel(@RequestBody EnrollStudentsResource enrollStudentsResource) {
        var enrollStudentsCommand = EnrollStudentsCommandFromResourceAssembler.toCommandFromResource(enrollStudentsResource);
        enrollmentCommandService.handle(enrollStudentsCommand);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResource("Students enrolled successfully."));
    }

    @Operation(description = "Assigns a teacher to classrooms.")
    @PostMapping("/teachers/assign")
    public ResponseEntity<MessageResource> assignTeacher(@RequestBody AssignTeacherResource assignTeacherResource) {
        var assignTeacherCommand = AssignTeacherCommandFromResourceAssembler.toCommandFromResource(assignTeacherResource);
        enrollmentCommandService.handle(assignTeacherCommand);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResource("Teacher assigned to classrooms successfully."));
    }

    @Operation(description = "Unenrolls a student from classrooms.")
    @DeleteMapping("/students/unenroll")
    public ResponseEntity<MessageResource> unenrollStudent(@RequestBody UnenrollStudentResource unassignUserResource) {
        var unenrollUserCommand = UnenrollStudentCommandFromResourceAssembler.toCommandFromResource(unassignUserResource);
        enrollmentCommandService.handle(unenrollUserCommand);
        return ResponseEntity.ok(new MessageResource("Student unenrolled from classrooms successfully."));
    }

    @Operation(description = "Unassigns a teacher from classrooms.")
    @DeleteMapping("/teachers/unassign")
    public ResponseEntity<MessageResource> unassignTeacher(@RequestBody UnassignTeacherResource unassignTeacherResource) {
        var unassignTeacherCommand = UnassignTeacherCommandFromResourceAssembler.toCommandFromResource(unassignTeacherResource);
        enrollmentCommandService.handle(unassignTeacherCommand);
        return ResponseEntity.ok(new MessageResource("Teacher unassigned from classrooms successfully."));
    }
}
