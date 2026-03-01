package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.model.queries.GetAcademicYearByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAllAcademicYearsQuery;
import com.hs.hstesis.learning.domain.services.AcademicYearQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicYearRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AcademicYearQueryServiceImpl implements AcademicYearQueryService {
    private final AcademicYearRepository academicYearRepository;

    public AcademicYearQueryServiceImpl(AcademicYearRepository academicYearRepository) {
        this.academicYearRepository = academicYearRepository;
    }

    @Override
    public Optional<AcademicYear> handle(GetAcademicYearByIdQuery query){
        return academicYearRepository.findById(query.id());
    }

    @Override
    public List<AcademicYear> handle(GetAllAcademicYearsQuery query){
        return academicYearRepository.findAll();
    }
}
