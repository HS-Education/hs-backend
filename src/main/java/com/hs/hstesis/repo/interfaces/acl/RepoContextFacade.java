package com.hs.hstesis.repo.interfaces.acl;

import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentChunkRepository;
import com.hs.hstesis.repo.domain.model.entities.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RepoContextFacade {
    private final DocumentChunkRepository documentChunkRepository;

    public RepoContextFacade(DocumentChunkRepository documentChunkRepository) {
        this.documentChunkRepository = documentChunkRepository;
    }

    public List<String> getDocumentChunksByTopicIds(List<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return List.of();
        
        return documentChunkRepository.findAllByDocumentTopicIdIn(topicIds)
                .stream()
                .map(DocumentChunk::getContent)
                .toList();
    }
}
