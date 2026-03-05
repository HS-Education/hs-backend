package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.DeleteClassroomCommand;
import com.hs.hstesis.learning.domain.model.commands.GenerateClassroomsCommand;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomsByUserIdQuery;
import com.hs.hstesis.learning.domain.services.ClassroomCommandService;
import com.hs.hstesis.learning.domain.services.ClassroomQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.ClassroomResource;
import com.hs.hstesis.learning.interfaces.rest.transform.ClassroomResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value ="/api/v1/classrooms", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Classrooms", description = "Classroom management endpoints")
public class ClassroomController {
    private final ClassroomCommandService classroomCommandService;
    private final ClassroomQueryService classroomQueryService;

    public ClassroomController(ClassroomCommandService classroomCommandService, ClassroomQueryService classroomQueryService) {
        this.classroomCommandService = classroomCommandService;
        this.classroomQueryService = classroomQueryService;
    }

    @PostMapping("/generate-all")
    public ResponseEntity<String> generateAllClassrooms() {
        int totalCreated = classroomCommandService.handle(new GenerateClassroomsCommand());
        if (totalCreated == 0) {
            return ResponseEntity.ok("No new classrooms were created (they already exist or some academic levels are incomplete).");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(String.format("%d classrooms generated successfully", totalCreated));
    }

    @GetMapping("/{classroomId}")
    public ResponseEntity<ClassroomResource> getClassroomById(@PathVariable Long classroomId) {
        var getClassroomByIdQuery = new GetClassroomByIdQuery(classroomId);
        var classroom = classroomQueryService.handle(getClassroomByIdQuery);

        if(classroom.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var classroomResource = ClassroomResourceFromEntityAssembler.toResourceFromEntity(classroom.get());
        return ResponseEntity.ok(classroomResource);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClassroomResource>> getClassroomsByUserId(@PathVariable Long userId) {
        var getClassroomsByUserIdQuery = new GetClassroomsByUserIdQuery(userId);
        var classrooms = classroomQueryService.handle(getClassroomsByUserIdQuery);

        var resources = classrooms.stream()
                .map(ClassroomResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }

    @DeleteMapping("/{classroomId}")
    public ResponseEntity<?> deleteClassroom(@PathVariable Long classroomId) {
        var command = new DeleteClassroomCommand(classroomId);
        classroomCommandService.handle(command);
        return ResponseEntity.ok("Classroom deleted successfully");
    }
}
