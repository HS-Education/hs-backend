package com.hs.hstesis.repo.application.internal.outboundservices.acl;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

@Service
public class ExternalIamService {
    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    public Long getAuthenticatedUserId() {
        return iamContextFacade.getAuthenticatedUserId();
    }

    public boolean hasRole(Long userId, String role) {
        return iamContextFacade.hasRole(userId, role);
    }
}