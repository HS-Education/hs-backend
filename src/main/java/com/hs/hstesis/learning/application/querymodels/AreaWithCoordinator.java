package com.hs.hstesis.learning.application.querymodels;

import com.hs.hstesis.learning.domain.model.entities.Area;

public record AreaWithCoordinator(Area area, String coordinatorName) {
}
