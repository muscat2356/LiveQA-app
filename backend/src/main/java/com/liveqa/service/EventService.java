package com.liveqa.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.liveqa.domain.Event;
import com.liveqa.domain.JoinCode;
import com.liveqa.domain.User;
import com.liveqa.dto.response.EventCreateResponse;
import com.liveqa.dto.response.EventSummaryResponse;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.EventRepository;

@Service
public class EventService {

    private final EventRepository eventRepository;

    private final UserService userService;

    public EventService(EventRepository eventRepository, UserService userService) {
        this.eventRepository = eventRepository;
        this.userService = userService;
    }

    public List<EventSummaryResponse> findAll(OidcUser oidc) {
        User user = requiredUser(oidc);
        return eventRepository.findByHost_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(e -> toEventSummaryResponse(e))
                .toList();
    }

    public EventCreateResponse save(OidcUser oidc, String title) {
        User user = requiredUser(oidc);
        JoinCode code = JoinCode.generate();
        if (existsByJoinCode(code)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "参加コードが重複しています。");
        }
        Event event = eventRepository.save(Event.create(user, title, code.code()));
        return toEventCreateResponse(event);
    }

    public User requiredUser(OidcUser oidc) {
        return userService.requireByOidcSubject(oidc.getSubject());
    }

    public boolean existsByJoinCode(JoinCode code) {
        return eventRepository.existsByJoinCode(code.code());
    }

    public EventSummaryResponse toEventSummaryResponse(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getJoinCode(),
                event.getStatus(),
                event.getCreatedAt());
    }

    public EventCreateResponse toEventCreateResponse(Event event) {
        return new EventCreateResponse(
                event.getId(),
                event.getTitle(),
                event.getJoinCode(),
                "/join/" + event.getJoinCode(),
                event.getStatus(),
                event.getCreatedAt());
    }
}
