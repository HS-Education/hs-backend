package com.hs.hstesis.learning.application.querymodels;

import com.hs.hstesis.learning.domain.model.entities.Area;

public record AreaWithCoordinatorQueryModel(Area area, String coordinatorName) {
}