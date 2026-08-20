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

/**
 * Eventsに対する業務処理を行うサービスクラス
 * EventService
 * @author ichikura
 */
@Service
public class EventService {

    private final EventRepository eventRepository;

    private final UserService userService;

    public EventService(EventRepository eventRepository, UserService userService) {
        this.eventRepository = eventRepository;
        this.userService = userService;
    }

    /**
     * ログイン中の主催者のイベント情報一覧を返すメソッド
     * OidcUserのsubjectからHost行を検索しなければNot Found
     * 確認したUser情報を用いてevents行を検索、0件でも空のListを返却
     * @param oidc SecurityContext内のOidcUser情報
     * @return 一覧Responseクラス
     */
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findAll(OidcUser oidc) {
        final User user = requiredUser(oidc);
        return eventRepository.findByHost_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(e -> toEventSummaryResponse(e))
                .toList();
    }

    /**
     * ログイン中の主催者の選択したイベント情報を返すメソッド
     * @param id イベントid
     * @param oidc SecurityContext内のOidcUser情報
     * @return 選択されたイベント情報Responseクラス
     */
    @Transactional(readOnly = true)
    public EventDetailResponse findById(Long id, OidcUser oidc) {
        return toEventDetailResponse(requireOwnedEvent(id, oidc));
    }

    /**
     * 入力された参加コードで対象のイベント情報を検索するメソッド
     * JoinCode.formで new　-> compact constructorで入力されたコードの検証、
     * 長さ不正・null・英数字以外はIllegalArgumentException
     * IllegalArgumentExceptionをキャッチしてBusinessExceptionへ変換
     * 
     * 参加コードで見つからない時は404
     * @param code 参加コード
     * @return 該当イベント情報Responseクラス
     */
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

    /**
     * イベント新規作成メソッド
     * OidcUserのsubjectからHost行を検索しなければNot Found
     * 参加コードを作成（重複時には三回まで再生成）
     * Eventのファクトリメソッドでインスタンス化し、save
     * @param oidc SecurityContext内のOidcUser情報
     * @param title 入力されたイベント名
     * @return 作成されたイベント情報Responseクラス
     */
    @Transactional
    public EventCreateResponse save(OidcUser oidc, String title) {
        final User user = requiredUser(oidc);
        final JoinCode code = generateJoinCode();
        final Event event = eventRepository.save(Event.create(user, title, code.code()));
        return toEventCreateResponse(event);
    }

    /**
     * イベントステータス更新メソッド
     * DRAFTへの遷移は不可 -> 送られてきたステータスを検証 400
     * 対象イベントを特定し、イベントの状態遷移の可否を検証 -> 409
     * 更新のファクトリメソッドでEventステータスを更新後save
     * @param id イベントid
     * @param status 遷移予定のステータス
     * @param oidc SecurityContext内のOidcUser情報
     * @return 更新したイベント情報Responseクラス
     */
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

    /**
     * OidcUserのsubjectでHost行を検索するメソッド
     * （UserServiceのメソッドを活用）
     * @param oidc SecurityContext内のOidcUser情報
     * @return DBのUser情報
     */
    public User requiredUser(OidcUser oidc) {
        return userService.requireByOidcSubject(oidc.getSubject());
    }

    /**
     * 参加コードを生成するメソッド
     * 生成はドメインのJoinCode内部で静的に行う
     * リトライ回数：3回 -> 上限に達した場合、409
     * @return 生成された参加コード
     */
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

    /**
     * ログイン中の主催者のイベントを特定するメソッド
     * 検証自体は他メソッドで行う
     * @param id イベントid
     * @param oidc SecurityContext内のOidcUser情報
     * @return 特定されたイベント情報
     */
    @Transactional(readOnly = true)
    protected Event requireOwnedEvent(Long id, OidcUser oidc) {
        final User user = requiredUser(oidc);
        return eventRepository.findByIdAndHost_Id(id, user.getId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "イベントが見つかりません。"));
    }

    /**
     * レスポンスDTOへ変換するメソッド
     * @param event DBから取得されたドメイン
     * @return レスポンス
     */
    public EventSummaryResponse toEventSummaryResponse(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getJoinCode(),
                event.getStatus(),
                event.getCreatedAt());
    }
    /**
     * レスポンスDTOへ変換するメソッド
     * @param event DBから取得されたドメイン
     * @return レスポンス
     */
    public EventCreateResponse toEventCreateResponse(Event event) {
        return new EventCreateResponse(
                event.getId(),
                event.getTitle(),
                event.getJoinCode(),
                "/join/" + event.getJoinCode(),
                event.getStatus(),
                event.getCreatedAt());
    }
    /**
     * レスポンスDTOへ変換するメソッド
     * @param event DBから取得されたドメイン
     * @return レスポンス
     */
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

     /**
     * レスポンスDTOへ変換するメソッド
     * @param event DBから取得されたドメイン
     * @return レスポンス
     */
    public EventJoinResponse toEventJoinResponse(Event event) {
        return new EventJoinResponse(
                event.getId(),
                event.getTitle(),
                event.getStatus());
    }

    /**
     * レスポンスDTOへ変換するメソッド
     * @param event DBから取得されたドメイン
     * @return レスポンス
     */
    public EventStatusResponse toEventStatusResponse(Event event) {
        return new EventStatusResponse(
                event.getId(),
                event.getStatus());
    }
}
