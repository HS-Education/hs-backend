package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.DeleteClassroomCommand;
import com.hs.hstesis.learning.domain.model.commands.GenerateClassroomsCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAllClassroomsQuery;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomMembersQuery;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomsByUserIdQuery;
import com.hs.hstesis.learning.domain.services.ClassroomCommandService;
import com.hs.hstesis.learning.domain.services.ClassroomQueryService;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.ClassroomResource;
import com.hs.hstesis.learning.interfaces.rest.resources.EnrollmentResource;
import com.hs.hstesis.learning.interfaces.rest.transform.ClassroomResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.EnrollmentResourceFromQueryModelAssembler;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/classrooms", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Classrooms", description = "Classroom management endpoints")
public class ClassroomController {
    private final ClassroomCommandService classroomCommandService;
    private final ClassroomQueryService classroomQueryService;
    private final EnrollmentQueryService enrollmentQueryService;

    public ClassroomController(ClassroomCommandService classroomCommandService,
            ClassroomQueryService classroomQueryService,
            EnrollmentQueryService enrollmentQueryService) {
        this.classroomCommandService = classroomCommandService;
        this.classroomQueryService = classroomQueryService;
        this.enrollmentQueryService = enrollmentQueryService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Generates classrooms for all academic levels based on existing study plans.")
    @PostMapping("/generate-all")
    public ResponseEntity<MessageResource> generateAllClassrooms() {
        int totalCreated = classroomCommandService.handle(new GenerateClassroomsCommand());
        if (totalCreated == 0) {
            return ResponseEntity.ok(new MessageResource(
                    "No new classrooms were created (they already exist or no study plan is configured)"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResource(String.format("%d classrooms generated successfully", totalCreated)));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('COORDINATOR') or @classroomSecurity.isMember(#classroomId)")
    @Operation(description = "Retrieves a classroom.")
    @GetMapping("/{classroomId}")
    public ResponseEntity<ClassroomResource> getClassroomById(@PathVariable Long classroomId) {
        var getClassroomByIdQuery = new GetClassroomByIdQuery(classroomId);
        var classroom = classroomQueryService.handle(getClassroomByIdQuery);

        if (classroom.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var classroomResource = ClassroomResourceFromEntityAssembler.toResourceFromEntity(
                classroom.get(), getTeacherName(classroomId));
        return ResponseEntity.ok(classroomResource);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('COORDINATOR') or (#userId != null and #userId == authentication.principal.id)")
    @Operation(description = "Retrieves all classrooms or filters them by userId")
    @GetMapping
    public ResponseEntity<List<ClassroomResource>> getClassrooms(
            @RequestParam(required = false) Long userId) {

        var classrooms = (userId == null)
                ? classroomQueryService.handle(new GetAllClassroomsQuery())
                : classroomQueryService.handle(new GetClassroomsByUserIdQuery(userId));

        var resources = classrooms.stream()
                .map(classroom -> ClassroomResourceFromEntityAssembler.toResourceFromEntity(
                        classroom, getTeacherName(classroom.getId())))
                .toList();

        return ResponseEntity.ok(resources);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('COORDINATOR') or @classroomSecurity.isMember(#classroomId)")
    @Operation(description = "Retrieves all members of a classroom")
    @GetMapping("/{classroomId}/members")
    public ResponseEntity<List<EnrollmentResource>> getClassroomMembers(@PathVariable Long classroomId) {

        var getClassroomMembersQuery = new GetClassroomMembersQuery(classroomId);
        var enrollments = enrollmentQueryService.handle(getClassroomMembersQuery);

        var resources = enrollments.stream()
                .map(EnrollmentResourceFromQueryModelAssembler::toResourceFromQueryModel)
                .toList();

        return ResponseEntity.ok(resources);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Deletes an existing classroom")
    @DeleteMapping("/{classroomId}")
    public ResponseEntity<MessageResource> deleteClassroom(@PathVariable Long classroomId) {
        var command = new DeleteClassroomCommand(classroomId);
        classroomCommandService.handle(command);
        return ResponseEntity.ok(new MessageResource("Classroom deleted successfully"));
    }

    private String getTeacherName(Long classroomId) {
        return enrollmentQueryService.handle(new GetClassroomMembersQuery(classroomId)).stream()
                .filter(member -> "TEACHER".equals(member.roleInClassroom()))
                .map(member -> member.userName())
                .findFirst()
                .orElse(null);
    }
}
