package com.hs.hstesis.achievements.interfaces.rest;

import com.hs.hstesis.achievements.domain.model.aggregates.AchievementInsight;
import com.hs.hstesis.achievements.domain.model.commands.GenerateClassroomInsightCommand;
import com.hs.hstesis.achievements.domain.services.AchievementCommandService;
import com.hs.hstesis.achievements.domain.services.AchievementQueryService;
import com.hs.hstesis.achievements.infrastructure.persistence.jpa.repositories.AchievementInsightRepository;
import com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade;
import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClassroomInsightAuthorizationTest {
    @Configuration @EnableMethodSecurity static class SecurityConfig {}
    private AnnotationConfigApplicationContext context;
    private AchievementController controller;
    private final LearningContextFacade learning = mock(LearningContextFacade.class);
    private final AchievementCommandService commands = mock(AchievementCommandService.class);

    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext();
        context.register(SecurityConfig.class);
        context.registerBean(AchievementController.class, () -> new AchievementController(
                mock(AchievementQueryService.class), commands, mock(AchievementInsightRepository.class),
                learning, mock(AssessmentsContextFacade.class)));
        context.refresh();
        controller = context.getBean(AchievementController.class);
        when(learning.canViewClassroomProgress(7L, 10L)).thenReturn(true);
        when(commands.handle(new GenerateClassroomInsightCommand(10L)))
                .thenReturn(new AchievementInsight("CLASSROOM", 10L, "Analysis"));
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); context.close(); }

    private UsernamePasswordAuthenticationToken authenticate(String... roles) {
        var authorities = Arrays.stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        var principal = new UserDetailsImpl(7L, "Teacher", "teacher", "unused", true, authorities, List.of(roles));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    @Test void coordinatorWhoIsAlsoTeacherCanGenerateForAccessibleClassroom() {
        var response = controller.generateClassroomInsight(10L, authenticate("COORDINATOR", "TEACHER"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(commands).handle(new GenerateClassroomInsightCommand(10L));
    }

    @Test void teacherCanStillGenerate() {
        assertThat(controller.generateClassroomInsight(10L, authenticate("TEACHER")).getStatusCode().value()).isEqualTo(200);
    }

    @Test void coordinatorWithoutTeacherRoleCannotGenerateClassroomInsight() {
        var auth = authenticate("COORDINATOR");
        assertThatThrownBy(() -> controller.generateClassroomInsight(10L, auth)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(commands);
    }

    @Test void dualRoleStillCannotGenerateForAnInaccessibleClassroom() {
        when(learning.canViewClassroomProgress(7L, 10L)).thenReturn(false);
        var auth = authenticate("COORDINATOR", "TEACHER");
        assertThatThrownBy(() -> controller.generateClassroomInsight(10L, auth)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(commands);
    }
}
