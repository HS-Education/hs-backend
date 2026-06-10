package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.domain.model.queries.*;
import com.hs.hstesis.learning.domain.services.ClassroomQueryService;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.ClassroomRepository;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.EnrollmentRepository;
import org.springframework.stereotype.Service;

import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.AreaRepository;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.CourseRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ClassroomQueryServiceImpl implements ClassroomQueryService {
    private final ClassroomRepository classroomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AreaRepository areaRepository;
    private final CourseRepository courseRepository;

    public ClassroomQueryServiceImpl(ClassroomRepository classroomRepository,
            EnrollmentRepository enrollmentRepository,
            AreaRepository areaRepository,
            CourseRepository courseRepository) {
        this.classroomRepository = classroomRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.areaRepository = areaRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public Optional<Classroom> handle(GetClassroomByIdQuery query) {
        return classroomRepository.findById(query.id());
    }

    @Override
    public List<Classroom> handle(GetClassroomsByUserIdQuery query) {
        Set<Classroom> classrooms = new HashSet<>();

        enrollmentRepository.findAllByUserId(query.userId())
                .stream()
                .map(Enrollment::getClassroom)
                .forEach(classrooms::add);

        Optional<Area> coordinatedArea = areaRepository.findByCoordinatorId(query.userId());
        if (coordinatedArea.isPresent()) {
            List<Course> courses = courseRepository.findAllByAreaId(coordinatedArea.get().getId());
            for (Course course : courses) {
                classrooms.addAll(classroomRepository.findAllByCourseId(course.getId()));
            }
        }

        return classrooms.stream().toList();
    }

    @Override
    public List<Classroom> handle(GetAllClassroomsQuery query) {
        return classroomRepository.findAll();
    }

    @Override
    public List<Classroom> handle(GetClassroomsByCourseIdQuery query) {
        return classroomRepository.findAllByCourseId(query.courseId());
    }
}