package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.commands.CreateCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateCourseCommand;
import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.domain.model.entities.Topic;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    public void addTopic(String name, Integer orderIndex) {
        Topic topic = new Topic(this, name, orderIndex);
        topics.add(topic);
    }
}