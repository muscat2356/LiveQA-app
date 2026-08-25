package com.liveqa.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liveqa.domain.Event;
import com.liveqa.domain.Poll;
import com.liveqa.domain.User;
import com.liveqa.dto.request.PollCreateRequest;
import com.liveqa.dto.response.PollResponse;
import com.liveqa.enums.PollStatus;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.EventRepository;
import com.liveqa.repository.PollRepository;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final EventRepository eventRepository;
    private final UserService userService;

    public PollService(PollRepository pollRepository, EventRepository eventRepository, UserService userService) {
        this.pollRepository = pollRepository;
        this.eventRepository = eventRepository;
        this.userService = userService;
    }


    @Transactional
    public PollResponse create(
        Long eventId,
        PollCreateRequest request,
        OidcUser oidc
    ) {
        final Event event = requireOwnedEvent(eventId, oidc);
        final String questionText = request.questionText().trim();
        final List<String> labels = normalizeLabels(request.options());
        final Poll saved = pollRepository.save(
            Poll.create(event, questionText, request.type(), labels)
        );
        
        return PollResponse.form(saved);

    }

    public List<PollResponse> findByEventId(Long eventId, OidcUser oidc) {

        if (oidc != null) {
            final User user =
             userService.requireByOidcSubject(oidc.getSubject());
            if (
                eventRepository.findByIdAndHost_Id(
                    eventId,
                    user.getId()).isPresent()
                ) {
                return pollRepository.findByEvent_IdOrderByCreatedAtAsc(eventId)
                        .stream()
                        .map(PollResponse::form)
                        .toList();
            }

        }
        requireVisibleEvent(eventId);
        return 
            pollRepository.findByEvent_IdAndStatusOrderByCreatedAtAsc(
                eventId,
                PollStatus.OPEN
            )
            .stream()
            .map(PollResponse::form)
            .toList();
        
    }

    @Transactional
    public PollResponse updateStatus(
        Long pollId,
        PollStatus status,
        OidcUser oidc
    ) {
        if (status == PollStatus.DRAFT) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "status は OPEN / CLOSED のいずれかです。");
        }
        final Poll poll = requireOwnedPoll(pollId, oidc);
        if (!poll.getStatus().canTransitionTo(status)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "この状態遷移はできません。");
        }
        poll.changeStatus(status);
        pollRepository.save(poll);
        // 配信（POLL_OPENED / POLL_CLOSED）は C。今は口だけ
        return PollResponse.form(poll);
    }

    private Event requireVisibleEvent(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    "イベントが見つかりません"
                ));
    }


    protected Event requireOwnedEvent(Long id, OidcUser oidc) {
        final User user = userService.requireByOidcSubject(oidc.getSubject());
        return eventRepository
                .findByIdAndHost_Id(id, user.getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "イベントが見つかりません。"));
    }

    /**
     * 所有ポールを復元するメソッド
     * 無い／他人所有のイベント配下は 404
     */
    private Poll requireOwnedPoll(Long pollId, OidcUser oidc) {
        final User user = userService.requireByOidcSubject(oidc.getSubject());
        return pollRepository
                .findByIdAndEvent_Host_Id(pollId, user.getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "ポールが見つかりません。"));
    }

    private List<String> normalizeLabels(List<String> options) {
        final List<String> trimmed = options.stream()
                .map(String::trim)
                .toList();
    
        if (trimmed.stream().distinct().count() < trimmed.size()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "選択肢ラベルが重複しています");
        }
    
        return trimmed; 
    }




}
