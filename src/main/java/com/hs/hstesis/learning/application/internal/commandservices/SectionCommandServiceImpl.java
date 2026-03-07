package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.AcademicLevelNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.SectionNameAlreadyExistsInAcademicLevelException;
import com.hs.hstesis.learning.domain.exceptions.SectionNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.domain.model.commands.CreateSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateSectionCommand;
import com.hs.hstesis.learning.domain.services.SectionCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicLevelRepository;
import com.hs.hstesis.learning.infrastructure.jpa.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SectionCommandServiceImpl implements SectionCommandService {
    private final SectionRepository sectionRepository;
    private final AcademicLevelRepository academicLevelRepository;

    public SectionCommandServiceImpl(SectionRepository sectionRepository, AcademicLevelRepository academicLevelRepository) {
        this.sectionRepository = sectionRepository;
        this.academicLevelRepository = academicLevelRepository;
    }

    @Override
    public Long handle(CreateSectionCommand command){
        var academicLevel = academicLevelRepository.findById(command.academicLevelId())
                .orElseThrow(() -> new AcademicLevelNotFoundException(command.academicLevelId()));
        if(sectionRepository.existsByName(command.name())){
            throw new SectionNameAlreadyExistsInAcademicLevelException(command.name(), academicLevel.getName());
        }
        var section = new Section(command, academicLevel);
        sectionRepository.save(section);
        return section.getId();
    }

    @Override
    public Optional<Section> handle(UpdateSectionCommand command){
        var section = sectionRepository.findById(command.id())
                .orElseThrow(() -> new SectionNotFoundException(command.id()));

        if(sectionRepository.existsByName(command.name())){
            throw new SectionNameAlreadyExistsInAcademicLevelException(command.name(), section.getAcademicLevel().getName());
        }

        section.update(command);
        sectionRepository.save(section);
        return Optional.of(section);
    }

    @Override
    public void handle(DeleteSectionCommand command){
        if(!sectionRepository.existsById(command.id())){
            throw new SectionNotFoundException(command.id());
        }
        sectionRepository.deleteById(command.id());
    }
}
