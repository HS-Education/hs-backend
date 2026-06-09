package com.hs.hstesis.repo.interfaces.acl;

import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentChunkRepository;
import com.hs.hstesis.repo.domain.model.entities.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.List;
import com.hs.hstesis.repo.interfaces.acl.dto.DocumentBasicData;

@Component
public class RepoContextFacade {
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public RepoContextFacade(DocumentChunkRepository documentChunkRepository, DocumentRepository documentRepository) {
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
    }

    public List<String> getDocumentChunksByTopicIds(List<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return List.of();
        
        return documentChunkRepository.findAllByDocumentTopicIdIn(topicIds)
                .stream()
                .map(DocumentChunk::getContent)
                .toList();
    }

    public List<DocumentBasicData> getDocumentBasicDataByCourseIdIn(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return List.of();
        return documentRepository.findAllByCourseIdIn(courseIds)
                .stream()
                .map(d -> {
                    // Extract a single courseId from its targets that matches the provided list
                    Long courseId = d.getTargets().stream()
                            .map(t -> t.getId().getCourseId())
                            .filter(courseIds::contains)
                            .findFirst()
                            .orElse(null);
                    return new DocumentBasicData(d.getId(), d.getTitle(), courseId);
                })
                .toList();
    }
}
