package com.hs.hstesis.learning.domain.model.commands;

public record RemoveCourseFromStudyPlanCommand(Long id){
    public RemoveCourseFromStudyPlanCommand {
        if(id == null || id <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
    }
}
