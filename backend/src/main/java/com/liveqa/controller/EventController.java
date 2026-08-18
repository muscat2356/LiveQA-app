package com.liveqa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.request.EventCreateRequest;
import com.liveqa.dto.response.EventCreateResponse;
import com.liveqa.dto.response.EventDetailResponse;
import com.liveqa.dto.response.EventSummaryResponse;
import com.liveqa.service.EventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<List<EventSummaryResponse>> findAll(@AuthenticationPrincipal OidcUser oidc) {
        return ResponseEntity.ok(eventService.findAll(oidc));
    }

    @PostMapping("/events")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<EventCreateResponse> create(
            @AuthenticationPrincipal OidcUser oidc,
            @Valid @RequestBody EventCreateRequest request
        ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.save(oidc, request.title()));
    }

    @GetMapping("/events/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<EventDetailResponse> findById(
            @AuthenticationPrincipal OidcUser oidc,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventService.findById(id, oidc));
    }


}
