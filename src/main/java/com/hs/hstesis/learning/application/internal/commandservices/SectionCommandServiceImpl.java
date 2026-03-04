package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.AcademicLevelNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.SectionNameAlreadyExistsException;
import com.hs.hstesis.learning.domain.exceptions.SectionNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.domain.model.commands.CreateSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.EditSectionNameCommand;
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
        var level = academicLevelRepository.findById(command.academicLevelId())
                .orElseThrow(() -> new AcademicLevelNotFoundException(command.academicLevelId()));
        if(sectionRepository.existsByName(command.name())){
            throw new SectionNameAlreadyExistsException(command.name());
        }
        var section = new Section(command, level);
        sectionRepository.save(section);
        return section.getId();
    }

    @Override
    public Optional<Section> handle(EditSectionNameCommand command){
        var section = sectionRepository.findById(command.id())
                .orElseThrow(() -> new SectionNotFoundException(command.id()));
        if(sectionRepository.existsByName(command.newName())){
            throw new SectionNameAlreadyExistsException(command.newName());
        }
        section.editName(command);
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
