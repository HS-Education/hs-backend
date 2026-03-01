package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;
import com.hs.hstesis.learning.domain.model.queries.GetAcademicLevelByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAllAcademicLevelsQuery;
import com.hs.hstesis.learning.domain.services.AcademicLevelQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicLevelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AcademicLevelQueryServiceImpl implements AcademicLevelQueryService {
    private final AcademicLevelRepository academicLevelRepository;

    public AcademicLevelQueryServiceImpl(AcademicLevelRepository academicLevelRepository) {
        this.academicLevelRepository = academicLevelRepository;
    }

    @Override
    public Optional<AcademicLevel> handle(GetAcademicLevelByIdQuery query){
        return academicLevelRepository.findById(query.id());
    }

    @Override
    public List<AcademicLevel> handle(GetAllAcademicLevelsQuery query){
        return academicLevelRepository.findAll();
    }
}
