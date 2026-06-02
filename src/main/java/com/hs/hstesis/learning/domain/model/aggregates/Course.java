package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.exceptions.TopicNotFoundException;
import com.hs.hstesis.learning.domain.model.commands.CreateCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateCourseCommand;
import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.domain.model.entities.Topic;
import com.hs.hstesis.repo.domain.exceptions.TopicAlreadyExistsException;
import com.hs.hstesis.shared.domain.model.util.TextUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Setter
@Getter
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Topic> topics = new ArrayList<>();

    protected Course() {}

    public Course(CreateCourseCommand command, Area area) {
        this.name = command.name().toUpperCase().trim();
        this.area = area;
    }

    public void update(UpdateCourseCommand command) {
        if(command.name() != null) {
            changeName(command.name().toUpperCase().trim());
        }
    }

    private void changeName(String name) {
        this.name = name;
    }

    public void addTopic(String name, com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod gradingPeriod) {
        String normalizedName = TextUtils.normalize(name);

        boolean exists = topics.stream()
                .filter(t -> t.getGradingPeriod().getId().equals(gradingPeriod.getId()))
                .anyMatch(t -> TextUtils.normalize(t.getName()).equals(normalizedName));

        if (exists) {
            throw new TopicAlreadyExistsException(name);
        }

        int nextIndex = (int) topics.stream()
                .filter(t -> t.getGradingPeriod().getId().equals(gradingPeriod.getId()))
                .count() + 1;
        Topic topic = new Topic(this, gradingPeriod, name.toUpperCase().trim(), nextIndex);
        topics.add(topic);
    }

    public void removeTopic(Long topicId) {
        Topic topic = topics.stream()
                .filter(t -> topicId.equals(t.getId()))
                .findFirst()
                .orElseThrow(() -> new TopicNotFoundException(topicId));

        topics.remove(topic);
    }

    public void reorderTopics(List<com.hs.hstesis.learning.domain.model.commands.TopicOrderDto> topicsOrder, com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.GradingPeriodRepository gradingPeriodRepository) {
        Map<Long, Topic> topicMap = topics.stream()
                .collect(Collectors.toMap(Topic::getId, t -> t));

        for (var orderDto : topicsOrder) {
            Topic topic = topicMap.get(orderDto.topicId());

            if (topic == null) {
                throw new TopicNotFoundException(orderDto.topicId());
            }

            if (!topic.getGradingPeriod().getId().equals(orderDto.gradingPeriodId())) {
                var newGradingPeriod = gradingPeriodRepository.findById(orderDto.gradingPeriodId())
                        .orElseThrow(() -> new IllegalArgumentException("Grading Period not found"));
                topic.updateGradingPeriod(newGradingPeriod);
            }

            topic.updateOrderIndex(orderDto.orderIndex());
        }
    }
}