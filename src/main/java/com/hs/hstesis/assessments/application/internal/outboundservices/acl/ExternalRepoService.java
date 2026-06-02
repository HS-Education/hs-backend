package com.hs.hstesis.assessments.application.internal.outboundservices.acl;

import com.hs.hstesis.repo.interfaces.acl.RepoContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("assessmentsExternalRepoService")
public class ExternalRepoService {
    private final RepoContextFacade repoContextFacade;

    public ExternalRepoService(RepoContextFacade repoContextFacade) {
        this.repoContextFacade = repoContextFacade;
    }

    public List<String> getDocumentChunksByTopicIds(List<Long> topicIds) {
        return repoContextFacade.getDocumentChunksByTopicIds(topicIds);
    }
}
