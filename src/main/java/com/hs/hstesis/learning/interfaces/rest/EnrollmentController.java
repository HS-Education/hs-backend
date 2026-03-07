package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.queries.GetClassroomMembersQuery;
import com.hs.hstesis.learning.domain.services.EnrollmentCommandService;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.*;
import com.hs.hstesis.learning.interfaces.rest.transform.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/enrollments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Enrollments", description = "Endpoints for managing student enrollments and teacher assignments")
public class EnrollmentController {
    private final EnrollmentCommandService enrollmentCommandService;
    private final EnrollmentQueryService enrollmentQueryService;

    public EnrollmentController(EnrollmentCommandService enrollmentCommandService, EnrollmentQueryService enrollmentQueryService) {
        this.enrollmentCommandService = enrollmentCommandService;
        this.enrollmentQueryService = enrollmentQueryService;
    }

    @PostMapping("/students")
    public ResponseEntity<String> enrollStudentsToAcademicLevel(@RequestBody EnrollStudentsResource enrollStudentsResource) {
        var enrollStudentsCommand = EnrollStudentsCommandFromResourceAssembler.toCommandFromResource(enrollStudentsResource);
        enrollmentCommandService.handle(enrollStudentsCommand);
        return ResponseEntity.status(HttpStatus.CREATED).body("Students enrolled successfully.");
    }

    @PostMapping("/teachers")
    public ResponseEntity<String> assignTeacher(@RequestBody AssignTeacherResource assignTeacherResource) {
        var assignTeacherCommand = AssignTeacherCommandFromResourceAssembler.toCommandFromResource(assignTeacherResource);
        enrollmentCommandService.handle(assignTeacherCommand);
        return ResponseEntity.status(HttpStatus.CREATED).body("Teacher assigned to classrooms.");
    }

    @GetMapping("/classroom/{classroomId}/members")
    public ResponseEntity<List<EnrollmentResource>> getClassroomMembers(@PathVariable Long classroomId) {

        var getClassroomMembersQuery = new GetClassroomMembersQuery(classroomId);
        var enrollments = enrollmentQueryService.handle(getClassroomMembersQuery);

        var resources = enrollments.stream()
                .map(EnrollmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }

    @DeleteMapping("/students/unenroll")
    public ResponseEntity<?> unenrollUser(@RequestBody UnassignUserResource unassignUserResource) {
        var unenrollUserCommand = UnenrollUserCommandFromResourceAssembler.toCommandFromResource(unassignUserResource);
        enrollmentCommandService.handle(unenrollUserCommand);
        return ResponseEntity.ok("User unenrolled from classroom.");
    }

    @DeleteMapping("/teachers/unassign")
    public ResponseEntity<?> unassignTeacher(@RequestBody UnassignTeacherResource unassignTeacherResource) {
        var unassignTeacherCommand = UnassignTeacherCommandFromResourceAssembler.toCommandFromResource(unassignTeacherResource);
        enrollmentCommandService.handle(unassignTeacherCommand);
        return ResponseEntity.ok("Teacher unassigned from classrooms.");
    }
}
