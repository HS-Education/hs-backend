package com.hs.hstesis.achievements.application.internal.commandservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.hstesis.achievements.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.achievements.application.internal.outboundservices.acl.ExternalRepoService;
import com.hs.hstesis.achievements.application.internal.outboundservices.ai.ExternalAiService;
import com.hs.hstesis.achievements.domain.model.commands.GenerateStudentInsightCommand;
import com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformance;
import com.hs.hstesis.achievements.domain.services.AchievementQueryService;
import com.hs.hstesis.achievements.infrastructure.persistence.jpa.repositories.AchievementInsightRepository;
import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class StudentInsightPrivacyTest {
    @Test
    void sendsStudentCodeButNeverTheRealNameInModelRequest() throws Exception {
        var insightRepository = mock(AchievementInsightRepository.class);
        var queryService = mock(AchievementQueryService.class);
        var restTemplate = mock(RestTemplate.class);
        var learningService = mock(ExternalLearningService.class);
        var repoService = mock(ExternalRepoService.class);
        var iamFacade = mock(IamContextFacade.class);
        var externalAiService = new ExternalAiService(restTemplate);
        ReflectionTestUtils.setField(externalAiService, "aiServiceUrl", "http://ai.test");
        ReflectionTestUtils.setField(externalAiService, "apiKey", "");

        when(queryService.handle(new GetStudentPerformanceQuery(42L))).thenReturn(Optional.of(
                new StudentPerformance(42L, "Private Real Name", 18.5, List.of())));
        when(iamFacade.fetchUserCodeById(42L)).thenReturn(Optional.of("STUDENT-042"));
        when(learningService.getEnrolledCourseIds(42L)).thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(), eq(ExternalAiService.GenerateResponse.class)))
                .thenReturn(new ExternalAiService.GenerateResponse("model", "Keep improving.", 1, 1));

        var service = new AchievementCommandServiceImpl(insightRepository, queryService, externalAiService,
                learningService, repoService, iamFacade);
        service.handle(new GenerateStudentInsightCommand(42L));

        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForObject(eq("http://ai.test/generate"), requestCaptor.capture(),
                eq(ExternalAiService.GenerateResponse.class));
        var entity = (HttpEntity<?>) requestCaptor.getValue();
        String requestJson = new ObjectMapper().writeValueAsString(entity.getBody());
        assertThat(requestJson).contains("STUDENT-042").doesNotContain("Private Real Name");
    }
}
