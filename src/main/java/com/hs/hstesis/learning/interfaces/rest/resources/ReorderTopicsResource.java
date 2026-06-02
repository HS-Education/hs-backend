package com.hs.hstesis.learning.interfaces.rest.resources;

import com.hs.hstesis.learning.domain.model.commands.TopicOrderDto;
import java.util.List;

public record ReorderTopicsResource(List<TopicOrderDto> topics) {}
