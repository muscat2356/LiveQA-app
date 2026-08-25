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

/**
 * Pollsに対する業務処理を行うサービスクラス
 * PollService
 * @author ichikura
 */
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

    /**
     * ポールを作成するメソッド
     * 所有イベントでなければ404。選択肢のtrim・重複はnormalizeLabels
     * @param eventId イベントid
     * @param request 設問文・種別・選択肢
     * @param oidc SecurityContext内のOidcUser情報
     * @return 作成したポール（DRAFT、得票は0）
     */
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

    /**
     * イベント配下のポール一覧を返すメソッド
     * 所有者は全件、それ以外はOPEN。イベントが無ければ404
     * PollResponse.form が LAZY の options を読む。
     * open-in-view: false のため Repository の find 後に Session が閉じると 500 になる。
     * 同一 TX で form まで終わらせる
     * @param eventId イベントid
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     * @return ポール一覧
     */
    @Transactional(readOnly = true)
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

    /**
     * ポールステータスを遷移するメソッド
     * DRAFT送りは400。遷移不可は409。所有でなければ404
     * @param pollId ポールid
     * @param status 遷移先（OPEN / CLOSED）
     * @param oidc SecurityContext内のOidcUser情報
     * @return 更新したポール
     */
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

    /**
     * 所有イベントを復元するメソッド
     * 無い／他人所有は404
     * @param id イベントid
     * @param oidc SecurityContext内のOidcUser情報
     * @return 所有イベント
     */
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
     * @param pollId ポールid
     * @param oidc SecurityContext内のOidcUser情報
     * @return 所有ポール
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
