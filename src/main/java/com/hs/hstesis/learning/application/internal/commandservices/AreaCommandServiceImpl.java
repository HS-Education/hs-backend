package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.commands.CreateAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAreaCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateAreaCommand;
import com.hs.hstesis.learning.domain.model.entities.Area;
import com.hs.hstesis.learning.domain.services.AreaCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AreaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AreaCommandServiceImpl implements AreaCommandService {
    private final AreaRepository areaRepository;

    public AreaCommandServiceImpl(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @Override
    public Long handle(CreateAreaCommand command){
        if(areaRepository.existsByName(command.name())){
            throw new AreaNameAlreadyExistsException(command.name());
        }
        var area = new Area(command);
        areaRepository.save(area);
        return area.getId();
    }

    @Override
    public Optional<Area> handle(UpdateAreaCommand command) {
        var area = areaRepository.findById(command.id())
                .orElseThrow(() -> new AreaNotFoundException(command.id()));

        if (command.name() != null && areaRepository.existsByName(command.name())) {
            throw new AreaNameAlreadyExistsException(command.name());
        }

        area.update(command);
        areaRepository.save(area);
        return Optional.of(area);
    }

    @Override
    public void handle(DeleteAreaCommand command){
        if (!areaRepository.existsById(command.id())) {
            throw new AreaNotFoundException(command.id());
        }
        areaRepository.deleteById(command.id());
    }
}
