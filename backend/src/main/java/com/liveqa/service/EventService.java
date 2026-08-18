package com.liveqa.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liveqa.domain.Event;
import com.liveqa.domain.JoinCode;
import com.liveqa.domain.User;
import com.liveqa.dto.response.EventCreateResponse;
import com.liveqa.dto.response.EventDetailResponse;
import com.liveqa.dto.response.EventJoinResponse;
import com.liveqa.dto.response.EventStatusResponse;
import com.liveqa.dto.response.EventSummaryResponse;
import com.liveqa.enums.EventStatus;
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

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findAll(OidcUser oidc) {
        final User user = requiredUser(oidc);
        return eventRepository.findByHost_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(e -> toEventSummaryResponse(e))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDetailResponse findById(Long id, OidcUser oidc) {
        return toEventDetailResponse(requireOwnedEvent(id, oidc));
    }

    @Transactional(readOnly = true)
    public EventJoinResponse findByJoinCode(String code) {
        final JoinCode joinCode;
        try {
            joinCode = JoinCode.form(code);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "参加コードが不正です。");
        }
        final Event event = eventRepository.findByJoinCode(joinCode.code())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "該当するルームが見つかりません。"));
        return toEventJoinResponse(event);
    }

    @Transactional
    public EventCreateResponse save(OidcUser oidc, String title) {
        final User user = requiredUser(oidc);
        final JoinCode code = generateJoinCode();
        final Event event = eventRepository.save(Event.create(user, title, code.code()));
        return toEventCreateResponse(event);
    }

    @Transactional
    public EventStatusResponse updateStatus(Long id, EventStatus status, OidcUser oidc) {
        if (status == EventStatus.DRAFT) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "status は OPEN / CLOSED のいずれかです。");
        }
        final Event event = requireOwnedEvent(id, oidc);
        if (!event.getStatus().canTransitionTo(status)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "この状態遷移はできません。");
        }
        event.changeStatus(status);
        eventRepository.save(event);
        return toEventStatusResponse(event);
    }

    public User requiredUser(OidcUser oidc) {
        return userService.requireByOidcSubject(oidc.getSubject());
    }

    public JoinCode generateJoinCode() {
        JoinCode code = JoinCode.generate();

        for (int i = 0; i < 3; i++) {
            if (eventRepository.existsByJoinCode(code.code())) {
                code = JoinCode.generate();
            } else {
                return code;
            }
        }
        throw new BusinessException(
                HttpStatus.CONFLICT,
                "Conflict",
                "コード再生成の上限に達しました。");
    }

    @Transactional(readOnly = true)
    protected Event requireOwnedEvent(Long id, OidcUser oidc) {
        final User user = requiredUser(oidc);
        final Event event = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "イベントが見つかりません。"));
        if (!event.getHost().getId().equals(user.getId())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Forbidden",
                    "権限がありません。");
        }
        return event;
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

    public EventDetailResponse toEventDetailResponse(Event event) {
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getJoinCode(),
                "/join/" + event.getJoinCode(),
                event.getStatus(),
                event.getQuestionCount(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    public EventJoinResponse toEventJoinResponse(Event event) {
        return new EventJoinResponse(
                event.getId(),
                event.getTitle(),
                event.getStatus());
    }

    public EventStatusResponse toEventStatusResponse(Event event) {
        return new EventStatusResponse(
                event.getId(),
                event.getStatus());
    }
}
