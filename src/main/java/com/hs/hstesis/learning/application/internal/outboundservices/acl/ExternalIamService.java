package com.hs.hstesis.learning.application.internal.outboundservices.acl;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service("learningExternalIamService")
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

    public Optional<String> fetchUserNameById(Long userId) {
        return iamContextFacade.fetchUserNameById(userId);
    }

    public Map<Long, String> fetchUserNamesByIds(Set<Long> userIds) {
        return iamContextFacade.fetchUserNamesByIds(userIds);
    }

    public List<Long> getMissingUsers(Set<Long> userIds) {
        return iamContextFacade.getMissingUsers(userIds);
    }

    public List<String> getUserNamesWithoutRole(Set<Long> userIds, String roleName) {
        return iamContextFacade.getUserNamesWithoutRole(userIds, roleName);
    }
}
