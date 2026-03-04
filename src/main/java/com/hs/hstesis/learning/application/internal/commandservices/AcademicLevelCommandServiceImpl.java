package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.AcademicLevelNameAlreadyExistsException;
import com.hs.hstesis.learning.domain.exceptions.AcademicLevelNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.AcademicLevelRelatedToSectionsException;
import com.hs.hstesis.learning.domain.model.commands.CreateAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.EditAcademicLevelNameCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;
import com.hs.hstesis.learning.domain.services.AcademicLevelCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicLevelRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AcademicLevelCommandServiceImpl implements AcademicLevelCommandService {
    private final AcademicLevelRepository academicLevelRepository;

    public AcademicLevelCommandServiceImpl(AcademicLevelRepository academicLevelRepository) {
        this.academicLevelRepository = academicLevelRepository;
    }

    @Override
    public Long handle(CreateAcademicLevelCommand command){
        if(academicLevelRepository.existsByName(command.name())){
            throw new AcademicLevelNameAlreadyExistsException(command.name());
        }
        var academicLevel = new AcademicLevel(command);
        academicLevelRepository.save(academicLevel);
        return academicLevel.getId();
    }

    @Override
    public Optional<AcademicLevel> handle(EditAcademicLevelNameCommand command){
        var academicLevel = academicLevelRepository.findById(command.id())
                .orElseThrow(() -> new AcademicLevelNotFoundException(command.id()));
        if(academicLevelRepository.existsByName(command.newName())){
            throw new AcademicLevelNameAlreadyExistsException(command.newName());
        }
        academicLevel.editName(command);
        academicLevelRepository.save(academicLevel);
        return Optional.of(academicLevel);
    }

    @Override
    public void handle(DeleteAcademicLevelCommand command){
        if (!academicLevelRepository.existsById(command.id())) {
            throw new AcademicLevelNotFoundException(command.id());
        }
        academicLevelRepository.deleteById(command.id());
    }
}
