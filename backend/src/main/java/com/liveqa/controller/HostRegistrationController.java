package com.liveqa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.request.HostRegisterRequest;
import com.liveqa.service.HostRegistrationService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * HOST メール発行 API。CSRF 検証はしない（発行専用）
 * HostRegistrationController
 * @author ichikura
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "認証", description = "HOST 発行 POST /api/auth/register。logout は Security の URL")
public class HostRegistrationController {

    private final HostRegistrationService hostRegistrationService;

    public HostRegistrationController(HostRegistrationService hostRegistrationService) {
        this.hostRegistrationService = hostRegistrationService;
    }

    /**
     * メールだけ受け取り、Keycloak へ HOST 発行を依頼するPOSTメソッド
     * @param request メール
     * @return 202 空
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody HostRegisterRequest request) {
        hostRegistrationService.register(request.email());
        return ResponseEntity.accepted().build();
    }
}
