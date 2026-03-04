package com.hs.hstesis.learning.interfaces.rest.transform;

import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.interfaces.rest.resources.CourseResource;

public class CourseResourceFromEntityAssembler {
    public static CourseResource toResourceFromEntity(Course entity){
        return new CourseResource(
                entity.getId(),
                entity.getName(),
                entity.getArea().getName());
    }
}
