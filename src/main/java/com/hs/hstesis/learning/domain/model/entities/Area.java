package com.hs.hstesis.learning.domain.model.entities;

import com.hs.hstesis.learning.domain.model.commands.CreateAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateAreaCommand;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne(mappedBy = "area", cascade = CascadeType.ALL, orphanRemoval = true)
    private AreaCoordinator coordinator;

    protected Area() {}

    public Area(CreateAreaCommand command){
        this.name = command.name();
    }

    public void update(UpdateAreaCommand command){
        if(command.name() != null){
            changeName(command.name());
        }
    }

    private void changeName(String name){
        this.name = name;
    }
}