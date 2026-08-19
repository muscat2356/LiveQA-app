package com.liveqa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.request.QuestionCreateRequest;
import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.service.QuestionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/events/{id}/questions")
    public ResponseEntity<QuestionResponse> create(
            @PathVariable Long id,
            @Valid @RequestBody QuestionCreateRequest request,
            @AuthenticationPrincipal OidcUser oidc,
            HttpSession session
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                questionService
                    .create(
                        id,
                        request,
                        VoterKeySources.form(oidc, session)
                    )
                );
        }
    
    @GetMapping("/events/{id}/questions")
    public ResponseEntity<List<QuestionResponse>> findByEventId(
        @PathVariable Long id,
        @AuthenticationPrincipal OidcUser oidc,
        HttpSession session
    ) {
        return ResponseEntity.ok(
            questionService.findByEventId(id, VoterKeySources.form(oidc, session))
        );
    }
    
}
