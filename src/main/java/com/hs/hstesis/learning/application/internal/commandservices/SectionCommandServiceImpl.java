package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.SectionNameAlreadyExistsInAcademicLevelException;
import com.hs.hstesis.learning.domain.exceptions.SectionNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.domain.model.commands.CreateSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateSectionCommand;
import com.hs.hstesis.learning.domain.services.AcademicYearStateValidator;
import com.hs.hstesis.learning.domain.services.SectionCommandService;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.SectionRepository;
import com.hs.hstesis.shared.domain.model.util.TextUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SectionCommandServiceImpl implements SectionCommandService {
    private final SectionRepository sectionRepository;
    private final AcademicYearStateValidator yearValidator;

    public SectionCommandServiceImpl(SectionRepository sectionRepository,
                                     AcademicYearStateValidator yearValidator) {
        this.sectionRepository = sectionRepository;
        this.yearValidator = yearValidator;
    }

    @Override
    public Long handle(CreateSectionCommand command){
        String nameToCreate = TextUtils.normalize(command.name());

        boolean alreadyExists = sectionRepository.findAllByEducationLevelAndGradeLevel(
                        command.educationLevel(), command.gradeLevel())
                .stream()
                .anyMatch(s -> TextUtils.normalize(s.getName()).equals(nameToCreate));

        if (alreadyExists) {
            throw new SectionNameAlreadyExistsInAcademicLevelException(
                    command.name(),
                    command.educationLevel().name() + " " + command.gradeLevel().name()
            );
        }

        var section = new Section(command);
        sectionRepository.save(section);
        return section.getId();
    }

    @Override
    public Optional<Section> handle(UpdateSectionCommand command){
        var section = sectionRepository.findById(command.id())
                .orElseThrow(() -> new SectionNotFoundException(command.id()));

        if (command.name() != null) {
            String newNormalizedName = TextUtils.normalize(command.name());
            String currentNormalizedName = TextUtils.normalize(section.getName());

            if (!newNormalizedName.equals(currentNormalizedName)) {

                boolean alreadyExists = sectionRepository.findAllByEducationLevelAndGradeLevel(
                                section.getEducationLevel(), section.getGradeLevel())
                        .stream()
                        .anyMatch(s -> TextUtils.normalize(s.getName()).equals(newNormalizedName));

                if (alreadyExists) {
                    throw new SectionNameAlreadyExistsInAcademicLevelException(
                            command.name(),
                            section.getEducationLevel().name() + " " + section.getGradeLevel().name()
                    );
                }
            }
        }

        section.update(command);
        sectionRepository.save(section);
        return Optional.of(section);
    }

    @Override
    public void handle(DeleteSectionCommand command){
        yearValidator.validateCurrentYearIsNotActive();

        if(!sectionRepository.existsById(command.id())){
            throw new SectionNotFoundException(command.id());
        }

        sectionRepository.deleteById(command.id());
    }
}
