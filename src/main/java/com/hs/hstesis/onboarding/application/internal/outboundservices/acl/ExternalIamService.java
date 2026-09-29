package com.hs.hstesis.onboarding.application.internal.outboundservices.acl;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("onboardingExternalIamService")
public class ExternalIamService {
    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    public List<Long> getActiveNonAdminUserIds() {
        return iamContextFacade.getActiveNonAdminUserIds();
    }
}
