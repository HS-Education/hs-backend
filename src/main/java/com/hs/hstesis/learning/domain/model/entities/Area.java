package com.hs.hstesis.learning.domain.model.entities;

import com.hs.hstesis.learning.domain.model.commands.CreateAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateAreaCommand;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "areas")
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "coordinator_id", unique = true)
    private Long coordinatorId;

    protected Area() {}

    public Area(CreateAreaCommand command){
        this.name = command.name().toUpperCase().trim();
        this.coordinatorId = command.coordinatorId();
    }

    public void update(UpdateAreaCommand command){
        if(command.name() != null){
            changeName(command.name().toUpperCase().trim());
        }
        if(command.coordinatorId() != null){
            reassignCoordinator(command.coordinatorId());
        }
    }

    private void changeName(String name){
        this.name = name;
    }

    private void reassignCoordinator(Long coordinatorId){
        this.coordinatorId = coordinatorId;
    }
}