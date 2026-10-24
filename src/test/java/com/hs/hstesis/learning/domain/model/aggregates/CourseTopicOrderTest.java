package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.commands.CreateCourseCommand;
import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseTopicOrderTest {
    @Test
    void addingAfterDeletionUsesTheFreeWeekInsteadOfDuplicatingAnOccupiedWeek() {
        var course = new Course(new CreateCourseCommand("Synthetic course", 1L), null);
        var period = new GradingPeriod(Bimester.FIRST, null);
        period.setId(1L);
        course.addTopic("First", period);
        course.addTopic("Second", period);
        course.getTopics().remove(0);
        course.addTopic("Replacement", period);
        assertThat(course.getTopics()).extracting(t -> t.getOrderIndex()).containsExactly(2, 1);
    }

    @Test
    void numberingStartsAtOneForEachGradingPeriod() {
        var course = new Course(new CreateCourseCommand("Synthetic course", 1L), null);
        var first = new GradingPeriod(Bimester.FIRST, null);
        first.setId(1L);
        var second = new GradingPeriod(Bimester.SECOND, null);
        second.setId(2L);
        course.addTopic("First period", first);
        course.addTopic("Second period", second);
        assertThat(course.getTopics()).extracting(t -> t.getOrderIndex()).containsExactly(1, 1);
    }
}
