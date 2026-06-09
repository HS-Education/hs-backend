package com.hs.hstesis.achievements.application.internal.outboundservices.acl;

import com.hs.hstesis.repo.interfaces.acl.RepoContextFacade;
import com.hs.hstesis.repo.interfaces.acl.dto.DocumentBasicData;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalRepoService {
    private final RepoContextFacade repoContextFacade;

    public ExternalRepoService(RepoContextFacade repoContextFacade) {
        this.repoContextFacade = repoContextFacade;
    }

    public List<DocumentBasicData> getDocumentsByCourseIds(List<Long> courseIds) {
        return repoContextFacade.getDocumentBasicDataByCourseIdIn(courseIds);
    }
}
