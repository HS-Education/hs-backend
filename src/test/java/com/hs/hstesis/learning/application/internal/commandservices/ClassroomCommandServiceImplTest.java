package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.NoAcademicYearReadyException;
import com.hs.hstesis.learning.domain.exceptions.StudyPlanEntryNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.commands.DeleteClassroomCommand;
import com.hs.hstesis.learning.domain.model.commands.GenerateClassroomsCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.model.valueobjects.*;
import com.hs.hstesis.learning.domain.services.AcademicYearStateValidator;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClassroomCommandServiceImplTest {
    private final SectionRepository sections = mock(SectionRepository.class);
    private final AcademicYearRepository years = mock(AcademicYearRepository.class);
    private final ClassroomRepository classrooms = mock(ClassroomRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final StudyPlanRepository plans = mock(StudyPlanRepository.class);
    private final AcademicYearStateValidator yearValidator = mock(AcademicYearStateValidator.class);
    private final ClassroomCommandServiceImpl service = new ClassroomCommandServiceImpl(
            sections, years, classrooms, enrollments, plans, yearValidator);

    private AcademicYear plannedYear() {
        var year = new AcademicYear(2030);
        year.setId(19L);
        var period = new GradingPeriod(Bimester.FIRST, year);
        period.configureDates(LocalDate.now().plusDays(30), LocalDate.now().plusDays(60));
        year.getPeriods().add(period);
        return year;
    }

    @Test
    void refusesGenerationWithoutFullyScheduledYear() {
        when(years.findAll()).thenReturn(List.of(new AcademicYear(2030)));
        assertThatThrownBy(() -> service.handle(new GenerateClassroomsCommand()))
                .isInstanceOf(NoAcademicYearReadyException.class);
        verifyNoInteractions(plans, sections, classrooms);
    }

    @Test
    void refusesGenerationWithoutStudyPlan() {
        when(years.findAll()).thenReturn(List.of(plannedYear()));
        when(plans.findAll()).thenReturn(List.of());
        assertThatThrownBy(() -> service.handle(new GenerateClassroomsCommand()))
                .isInstanceOf(StudyPlanEntryNotFoundException.class);
        verifyNoInteractions(sections, classrooms);
    }

    @Test
    void repeatGenerationDoesNotDuplicateClassrooms() {
        var year = plannedYear();
        var course = mock(Course.class);
        when(course.getId()).thenReturn(7L);
        var section = mock(Section.class);
        when(section.getId()).thenReturn(8L);
        when(years.findAll()).thenReturn(List.of(year));
        when(plans.findAll()).thenReturn(List.of(new StudyPlan(EducationLevel.SECONDARY, GradeLevel.SECOND, course)));
        when(sections.findAllByEducationLevelAndGradeLevel(EducationLevel.SECONDARY, GradeLevel.SECOND))
                .thenReturn(List.of(section));
        when(classrooms.existsByCourseIdAndSectionIdAndAcademicYearId(7L, 8L, 19L))
                .thenReturn(false, true);

        assertThat(service.handle(new GenerateClassroomsCommand())).isEqualTo(1);
        assertThat(service.handle(new GenerateClassroomsCommand())).isZero();
        verify(classrooms, times(1)).save(any(Classroom.class));
    }

    @Test
    void deletingClassroomRemovesEnrollmentLinksFirst() {
        var classroom = mock(Classroom.class);
        when(classroom.getId()).thenReturn(5L);
        when(classroom.getStatus()).thenReturn(ClassroomStatus.INACTIVE);
        when(classrooms.findById(5L)).thenReturn(Optional.of(classroom));

        service.handle(new DeleteClassroomCommand(5L));

        var order = inOrder(yearValidator, enrollments, classrooms);
        order.verify(yearValidator).validateCurrentYearIsNotActive();
        order.verify(enrollments).deleteAllByClassroomId(5L);
        order.verify(classrooms).delete(classroom);
    }
}
