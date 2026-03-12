package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.aggregates.Section;
import com.hs.hstesis.learning.domain.model.queries.GetAllSectionsQuery;
import com.hs.hstesis.learning.domain.model.queries.GetSectionByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetSectionsByEducationAndGradeLevelQuery;
import com.hs.hstesis.learning.domain.services.SectionQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SectionQueryServiceImpl implements SectionQueryService {
    private final SectionRepository sectionRepository;

    public SectionQueryServiceImpl(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    @Override
    public Optional<Section> handle(GetSectionByIdQuery query){
        return sectionRepository.findById(query.id());
    }

    @Override
    public List<Section> handle(GetAllSectionsQuery query){
        return sectionRepository.findAll();
    }

    @Override
    public List<Section> handle(GetSectionsByEducationAndGradeLevelQuery query){
        return sectionRepository.findAllByEducationLevelAndGradeLevel(query.educationLevel(), query.gradeLevel());
    }
}
