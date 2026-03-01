package com.hs.hstesis.learning.domain.model.entities;

import com.hs.hstesis.learning.domain.model.commands.CreateAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.EditAreaNameCommand;
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

    protected Area() {};

    public Area(CreateAreaCommand command){
        this.name = command.name();
    }

    public void editName(EditAreaNameCommand command){
        this.name = command.newName();
    }
}