package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.DeleteCourseCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAllCoursesQuery;
import com.hs.hstesis.learning.domain.model.queries.GetCourseByIdQuery;
import com.hs.hstesis.learning.domain.services.CourseCommandService;
import com.hs.hstesis.learning.domain.services.CourseQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.CourseResource;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateCourseResource;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateCourseResource;
import com.hs.hstesis.learning.interfaces.rest.transform.CourseResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.CreateCourseCommandFromResourceAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.UpdateCourseCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/create")
    public ResponseEntity<CourseResource> createCourse(@RequestBody CreateCourseResource createCourseResource){
        var createCourseCommand = CreateCourseCommandFromResourceAssembler.toCommandFromResource(createCourseResource);
        var courseId = courseCommandService.handle(createCourseCommand);

        if (courseId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getCourseByIdQuery = new GetCourseByIdQuery(courseId);
        var course = courseQueryService.handle(getCourseByIdQuery);

        if (course.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var courseResource = CourseResourceFromEntityAssembler.toResourceFromEntity(course.get());
        return new ResponseEntity<>(courseResource, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CourseResource>> getAllCourses() {
        var getAllCoursesQuery = new GetAllCoursesQuery();
        var courses = courseQueryService.handle(getAllCoursesQuery);
        var courseResources = courses.stream()
                .map(CourseResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(courseResources);
    }

    @PutMapping("/update-name/{courseId}")
    public ResponseEntity<CourseResource> updateCourseName(@PathVariable Long courseId, @RequestBody UpdateCourseResource updateCourseResource) {
        var updateCourseCommand = UpdateCourseCommandFromResourceAssembler.toCommandFromResource(courseId, updateCourseResource);
        var updatedCourse = courseCommandService.handle(updateCourseCommand);

        if (updatedCourse.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var courseResource = CourseResourceFromEntityAssembler.toResourceFromEntity(updatedCourse.get());
        return ResponseEntity.ok(courseResource);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId) {
        var deleteCourseCommand = new DeleteCourseCommand(courseId);
        courseCommandService.handle(deleteCourseCommand);
        return ResponseEntity.ok("Course deleted successfully");
    }
}
