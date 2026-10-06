package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.commands.UpdateGradingPeriodCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodByIdQuery;
import com.hs.hstesis.learning.domain.services.*;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.GradingPeriodRepository;
import com.hs.hstesis.learning.interfaces.rest.AcademicYearController;
import com.hs.hstesis.shared.interfaces.rest.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GradingPeriodCommandServiceImplTest {
    private final GradingPeriodRepository periods = mock(GradingPeriodRepository.class);
    private final GradingPeriodCommandServiceImpl service = new GradingPeriodCommandServiceImpl(periods);
    private GradingPeriod period;
    private LocalDate today;
    private LocalDate start;
    private MockedStatic<LocalDate> dates;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        today = LocalDate.of(2030, 3, 1);
        start = LocalDate.of(2030, 4, 1);
        dates = mockStatic(LocalDate.class, CALLS_REAL_METHODS);
        dates.when(LocalDate::now).thenReturn(today);
        var year = new AcademicYear(today.getYear());
        year.setId(1L);
        period = new GradingPeriod(Bimester.SECOND, year);
        period.setId(2L);
        when(periods.findByIdAndAcademicYearId(2L, 1L)).thenReturn(Optional.of(period));
        when(periods.findAllByAcademicYearId(1L)).thenReturn(List.of(period));
        var queries = mock(GradingPeriodQueryService.class);
        when(queries.handle(any(GetGradingPeriodByIdQuery.class))).thenAnswer(ignored -> Optional.of(period));
        // Real controller, resource assembler, service and exception advice; no production writes.
        mvc = standaloneSetup(new AcademicYearController(mock(AcademicYearCommandService.class),
                mock(AcademicYearQueryService.class), queries, service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach void restoreClock() { dates.close(); }

    @ParameterizedTest @ValueSource(ints = {14, 15, 20, 21, 28})
    void acceptsTwoCompleteWeeksAndPreviouslyValidLongerPeriods(int days) {
        var end = start.plusDays(days);
        assertThat(service.handle(command(start, end))).contains(period);
        assertThat(period.getStartDate()).isEqualTo(start);
        assertThat(period.getEndDate()).isEqualTo(end);
        verify(periods).save(period);
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 7, 13})
    void rejectsShorterPeriodsWithoutMutatingOrSaving(int days) {
        assertThatThrownBy(() -> service.handle(command(start, start.plusDays(days))))
                .isInstanceOf(GradingPeriodDurationTooShortException.class)
                .hasMessage("A grading period must last at least 2 weeks. Current duration: '%d' weeks.", days / 7);
        assertThat(period.getStartDate()).isNull();
        assertThat(period.getEndDate()).isNull();
        verify(periods, never()).save(any());
    }

    @Test void allowsAnActivePeriodToEndAfterExactlyTwoWeeks() {
        var originalStart = today.minusDays(7);
        period.configureDates(originalStart, today.plusDays(21));
        service.handle(command(originalStart, originalStart.plusDays(14)));
        assertThat(period.getEndDate()).isEqualTo(today.plusDays(7));
        verify(periods).save(period);
    }

    @Test void activePeriodStartCannotBeChanged() {
        period.configureDates(today.minusDays(7), today.plusDays(21));
        assertThatThrownBy(() -> service.handle(command(today, today.plusDays(14))))
                .isInstanceOf(StartedGradingPeriodModificationException.class);
        verify(periods, never()).save(any());
    }

    @Test void finishedPeriodsCannotBeChanged() {
        period.configureDates(today.minusDays(30), today.minusDays(1));
        assertThatThrownBy(() -> service.handle(command(start, start.plusDays(14))))
                .isInstanceOf(FinishedGradingPeriodModificationException.class);
        verify(periods, never()).save(any());
    }

    @Test void newPeriodsCannotStartInThePast() {
        assertThatThrownBy(() -> service.handle(command(today.minusDays(1), today.plusDays(13))))
                .isInstanceOf(GradingPeriodInPastException.class);
        verify(periods, never()).save(any());
    }

    @Test void datesMustRemainInTheCurrentYear() {
        var nextYear = start.plusYears(1);
        assertThatThrownBy(() -> service.handle(command(nextYear, nextYear.plusDays(14))))
                .isInstanceOf(InvalidGradingPeriodYearException.class);
        verify(periods, never()).save(any());
    }

    @Test void reversedDatesAreStillRejected() {
        assertThatThrownBy(() -> command(start, start.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        verify(periods, never()).save(any());
    }

    @Test void aTwoWeekPeriodCannotOverlapItsPredecessor() {
        var previous = otherPeriod(1L, Bimester.FIRST, start.minusDays(14), start);
        when(periods.findAllByAcademicYearId(1L)).thenReturn(List.of(previous, period));
        assertThatThrownBy(() -> service.handle(command(start, start.plusDays(14))))
                .isInstanceOf(BimesterSequencePredecessorException.class);
        verify(periods, never()).save(any());
    }

    @Test void aTwoWeekPeriodCannotOverlapItsSuccessor() {
        var next = otherPeriod(3L, Bimester.THIRD, start.plusDays(14), start.plusDays(28));
        when(periods.findAllByAcademicYearId(1L)).thenReturn(List.of(period, next));
        assertThatThrownBy(() -> service.handle(command(start, start.plusDays(14))))
                .isInstanceOf(BimesterSequenceSuccessorException.class);
        verify(periods, never()).save(any());
    }

    @Test void acceptsTwoWeeksBetweenAdjacentNonoverlappingPeriods() {
        var previous = otherPeriod(1L, Bimester.FIRST, start.minusDays(15), start.minusDays(1));
        var next = otherPeriod(3L, Bimester.THIRD, start.plusDays(15), start.plusDays(29));
        when(periods.findAllByAcademicYearId(1L)).thenReturn(List.of(previous, period, next));
        assertThat(service.handle(command(start, start.plusDays(14)))).contains(period);
        verify(periods).save(period);
    }

    @Test void periodsMustBelongToTheRequestedAcademicYear() {
        when(periods.findByIdAndAcademicYearId(2L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.handle(command(start, start.plusDays(14))))
                .isInstanceOf(GradingPeriodNotFoundException.class);
        verify(periods, never()).save(any());
    }

    @Test void actualUpdateEndpointReturnsTheAcceptedTwoWeekDates() throws Exception {
        mvc.perform(put("/api/v1/academic-years/1/grading-periods/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2030-04-01\",\"endDate\":\"2030-04-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2030-04-01"))
                .andExpect(jsonPath("$.endDate").value("2030-04-15"));
        verify(periods).save(period);
    }

    @Test void actualUpdateEndpointReturns400AndTheNewMinimumForThirteenDays() throws Exception {
        mvc.perform(put("/api/v1/academic-years/1/grading-periods/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2030-04-01\",\"endDate\":\"2030-04-14\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A grading period must last at least 2 weeks. Current duration: '1' weeks."));
        verify(periods, never()).save(any());
    }

    private UpdateGradingPeriodCommand command(LocalDate from, LocalDate to) {
        return new UpdateGradingPeriodCommand(1L, 2L, from, to);
    }

    private GradingPeriod otherPeriod(Long id, Bimester bimester, LocalDate from, LocalDate to) {
        var other = new GradingPeriod(bimester, period.getAcademicYear());
        other.setId(id);
        other.configureDates(from, to);
        return other;
    }
}
