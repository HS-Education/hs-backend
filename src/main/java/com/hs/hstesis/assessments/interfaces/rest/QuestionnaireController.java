package com.hs.hstesis.assessments.interfaces.rest;

import com.hs.hstesis.assessments.domain.model.commands.GenerateQuestionnaireCommand;
import com.hs.hstesis.assessments.domain.model.queries.GetQuestionnaireInstancesByStudentIdQuery;
import com.hs.hstesis.assessments.domain.model.queries.GetQuestionsByQuestionnaireInstanceIdQuery;
import com.hs.hstesis.assessments.domain.services.QuestionnaireCommandService;
import com.hs.hstesis.assessments.domain.services.QuestionnaireQueryService;
import com.hs.hstesis.assessments.interfaces.rest.resources.QuestionResource;
import com.hs.hstesis.assessments.interfaces.rest.resources.SubmitQuestionnaireResource;
import com.hs.hstesis.assessments.interfaces.rest.transform.QuestionResourceFromEntityAssembler;
import com.hs.hstesis.assessments.interfaces.rest.transform.SubmitQuestionnaireCommandFromResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assessments/questionnaires")
public class QuestionnaireController {

    private final QuestionnaireCommandService questionnaireCommandService;
    private final QuestionnaireQueryService questionnaireQueryService;

    public QuestionnaireController(QuestionnaireCommandService questionnaireCommandService,
                                   QuestionnaireQueryService questionnaireQueryService) {
        this.questionnaireCommandService = questionnaireCommandService;
        this.questionnaireQueryService = questionnaireQueryService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('TEACHER') or hasRole('COORDINATOR')")
    public ResponseEntity<Void> generateQuestionnaire(@RequestBody GenerateQuestionnaireCommand command) {
        questionnaireCommandService.handle(command);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/{questionnaireInstanceId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> submitQuestionnaire(
            @PathVariable Long questionnaireInstanceId,
            @RequestBody SubmitQuestionnaireResource resource) {
        var command = SubmitQuestionnaireCommandFromResourceAssembler.toCommandFromResource(questionnaireInstanceId, resource);
        questionnaireCommandService.handle(command);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/{questionnaireId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Long> startQuestionnaire(
            @PathVariable Long questionnaireId,
            org.springframework.security.core.Authentication authentication) {
            
        var userDetails = (com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl) authentication.getPrincipal();
        Long studentId = userDetails.getId();
        
        var command = new com.hs.hstesis.assessments.domain.model.commands.StartQuestionnaireCommand(questionnaireId, studentId);
        Long instanceId = questionnaireCommandService.handle(command);
        
        return ResponseEntity.ok(instanceId);
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<com.hs.hstesis.assessments.interfaces.rest.resources.AvailableQuestionnaireResource>> getAvailableQuestionnaires(org.springframework.security.core.Authentication authentication) {
        var userDetails = (com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl) authentication.getPrincipal();
        Long studentId = userDetails.getId();
        
        var query = new com.hs.hstesis.assessments.domain.model.queries.GetAvailableQuestionnairesQuery(studentId);
        var resources = questionnaireQueryService.handle(query);
        
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{questionnaireInstanceId}/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<QuestionResource>> getQuestionsForInstance(
            @PathVariable Long questionnaireInstanceId,
            org.springframework.security.core.Authentication authentication) {
            
        var userDetails = (com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl) authentication.getPrincipal();
        Long studentId = userDetails.getId();

        var instancesQuery = new GetQuestionnaireInstancesByStudentIdQuery(studentId);
        var myInstances = questionnaireQueryService.handle(instancesQuery);
        
        boolean ownsInstance = myInstances.stream()
                .anyMatch(instance -> instance.getId().equals(questionnaireInstanceId));
                
        if (!ownsInstance) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this questionnaire instance.");
        }

        var query = new GetQuestionsByQuestionnaireInstanceIdQuery(questionnaireInstanceId);
        var questions = questionnaireQueryService.handle(query);
        
        var resources = questions.stream()
                .map(QuestionResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{questionnaireInstanceId}/results")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<com.hs.hstesis.assessments.interfaces.rest.resources.QuestionnaireSubmissionResource> getSubmissionResults(
            @PathVariable Long questionnaireInstanceId,
            org.springframework.security.core.Authentication authentication) {
            
        var userDetails = (com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl) authentication.getPrincipal();
        Long studentId = userDetails.getId();

        var query = new com.hs.hstesis.assessments.domain.model.queries.GetSubmissionByInstanceIdQuery(questionnaireInstanceId, studentId);
        var submissionOpt = questionnaireQueryService.handle(query);
        
        if (submissionOpt.isEmpty()) {
            throw new com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException("Submission not found for this instance.");
        }
        
        var submission = submissionOpt.get();
        var answerResources = submission.getAnswers().stream()
                .map(a -> new com.hs.hstesis.assessments.interfaces.rest.resources.SubmissionAnswerResource(
                        a.getQuestion().getId(),
                        a.getQuestion().getText(),
                        a.getQuestion().getOptions(),
                        a.getSelectedOptionIndex(),
                        a.getQuestion().getCorrectOptionIndex(),
                        a.getIsCorrect(),
                        a.getAiFeedback()
                ))
                .collect(Collectors.toList());
                
        var resource = new com.hs.hstesis.assessments.interfaces.rest.resources.QuestionnaireSubmissionResource(
                submission.getScore(),
                answerResources
        );
        
        return ResponseEntity.ok(resource);
    }
}
