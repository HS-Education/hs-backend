package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.DeleteCourseCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAllCoursesQuery;
import com.hs.hstesis.learning.domain.model.queries.GetCourseByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetCoursesByAreaIdQuery;
import com.hs.hstesis.learning.domain.services.CourseCommandService;
import com.hs.hstesis.learning.domain.services.CourseQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.CourseResource;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateCourseResource;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateCourseResource;
import com.hs.hstesis.learning.interfaces.rest.transform.CourseResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.CreateCourseCommandFromResourceAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.UpdateCourseCommandFromResourceAssembler;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping(value = "/api/v1/courses", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Courses", description = "Course management endpoints")
public class CourseController {
    private final CourseCommandService courseCommandService;
    private final CourseQueryService courseQueryService;

    public CourseController(CourseCommandService courseCommandService, CourseQueryService courseQueryService) {
        this.courseCommandService = courseCommandService;
        this.courseQueryService = courseQueryService;
    }

    @Operation(description = "Create a new course.")
    @PostMapping()
    public ResponseEntity<CourseResource> createCourse(@RequestBody CreateCourseResource createCourseResource){
        var createCourseCommand = CreateCourseCommandFromResourceAssembler.toCommandFromResource(createCourseResource);
        var courseId = courseCommandService.handle(createCourseCommand);

        if (courseId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getCourseByIdQuery = new GetCourseByIdQuery(courseId);
        var course = courseQueryService.handle(getCourseByIdQuery);

        if (course.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var courseResource = CourseResourceFromEntityAssembler.toResourceFromEntity(course.get());
        return new ResponseEntity<>(courseResource, HttpStatus.CREATED);
    }

    @Operation(description = "Return a list of courses. Can be optionally filtered by areaId.")
    @GetMapping
    public ResponseEntity<List<CourseResource>> getCourses(
            @RequestParam(name = "areaId", required = false) Long areaId) {

        var courses = (areaId == null)
                ? courseQueryService.handle(new GetAllCoursesQuery())
                : courseQueryService.handle(new GetCoursesByAreaIdQuery(areaId));

        if (courses.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var courseResources = courses.stream()
                .map(CourseResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(courseResources);
    }

    @Operation(description = "Updates the details of an existing course.")
    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseResource> updateCourse(@PathVariable Long courseId, @RequestBody UpdateCourseResource updateCourseResource) {
        var updateCourseCommand = UpdateCourseCommandFromResourceAssembler.toCommandFromResource(courseId, updateCourseResource);
        var updatedCourse = courseCommandService.handle(updateCourseCommand);

        if (updatedCourse.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var courseResource = CourseResourceFromEntityAssembler.toResourceFromEntity(updatedCourse.get());
        return ResponseEntity.ok(courseResource);
    }

    @Operation(description = "Deletes an existing course.")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<MessageResource> deleteCourse(@PathVariable Long courseId) {
        var deleteCourseCommand = new DeleteCourseCommand(courseId);
        courseCommandService.handle(deleteCourseCommand);
        return ResponseEntity.ok(new MessageResource("Course deleted successfully."));
    }
}
