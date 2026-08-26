package com.liveqa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liveqa.domain.Event;
import com.liveqa.domain.Poll;
import com.liveqa.domain.PollOption;
import com.liveqa.domain.PollVote;
import com.liveqa.domain.User;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.request.PollCreateRequest;
import com.liveqa.dto.request.PollRespondRequest;
import com.liveqa.dto.response.PollResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.EventRepository;
import com.liveqa.repository.PollRepository;
import com.liveqa.repository.PollVoteRepository;
import com.liveqa.repository.projection.OptionVoteCount;

/**
 * Pollsに対する業務処理を行うサービスクラス
 * PollService
 * @author ichikura
 */
@Service
public class PollService {

    private static final String POLL_VOTE_UNIQUE = "uq_poll_responses_option_voter";

    private final PollRepository pollRepository;
    private final EventRepository eventRepository;
    private final UserService userService;
    private final PollVoteRepository pollVoteRepository;
    private final VoterKeyResolver voterKeyResolver;
    
    public PollService(
            PollRepository pollRepository,
            EventRepository eventRepository,
            UserService userService,
            PollVoteRepository pollVoteRepository,
            VoterKeyResolver voterKeyResolver
    ) {
        this.pollRepository = pollRepository;
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.pollVoteRepository = pollVoteRepository;
        this.voterKeyResolver = voterKeyResolver;
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

    /**
     * ポールへ回答するメソッド
     * voter_keyはSession。ポールとルームが両方OPENのときだけ可。置換しない
     * 同一ポールの回答は悲観ロックで直列化する（SINGLEの並行二重を防ぐ）
     * @param pollId ポールid
     * @param request 選んだ選択肢id
     * @param source 投票キーの取得元
     * @return 最新集計のポール
     */
    @Transactional
    public PollResponse respond(
            Long pollId,
            PollRespondRequest request,
            VoterKeySource source
    ) {
        final VoterKey voterKey = voterKeyResolver.resolve(source);
        final Poll poll = requireLockedPoll(pollId);
        requireAccepting(poll);
        final List<PollOption> selected = resolveOptions(poll, request.optionIds());
        final List<Long> alreadyChosen = myOptionIds(pollId, voterKey.value());

        validateNotAlreadyAnswered(poll, alreadyChosen, selected);

        try {
            final List<PollVote> votes = selected.stream()
                    .map(option -> PollVote.cast(option, voterKey.value()))
                    .toList();
            pollVoteRepository.saveAllAndFlush(votes);
        } catch (DataIntegrityViolationException ex) {
            if (isPollVoteUniqueViolation(ex)) {
                throw alreadyAnswered();
            }
            throw ex;
        }
        // 配信（POLL_UPDATED）は C。今は口だけ
        final List<Long> myOptionIds = Stream.concat(
                        alreadyChosen.stream(),
                        selected.stream().map(PollOption::getId))
                .toList();
        return PollResponse.form(poll, countByOptionId(pollId), myOptionIds);
    }

    /**
     * ポールの得票を返すメソッド
     * 公開。所有者以外のDRAFTは404。キー未確立でも200（myOptionIdsは空）
     * open-in-view: false のため options / event を同一TXで読む
     * @param pollId ポールid
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     * @param source 投票キーの取得元
     * @return 最新集計のポール
     */
    @Transactional(readOnly = true)
    public PollResponse findResults(
            Long pollId,
            OidcUser oidc,
            VoterKeySource source
    ) {
        final Poll poll = requireVisiblePoll(pollId);
        if (poll.getStatus() == PollStatus.DRAFT) {
            requireOwnedDraft(pollId, oidc);
        }
        final List<Long> myOptionIds = voterKeyResolver.tryResolve(source)
                .map(voterKey -> myOptionIds(pollId, voterKey.value()))
                .orElseGet(List::of);
        return PollResponse.form(poll, countByOptionId(pollId), myOptionIds);
    }

    /**
     * 下書きの得票を見てよいかを検証するメソッド
     * 未ログイン／他人所有はどちらも404（一覧と同じ見え方）
     * @param pollId ポールid
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     */
    private void requireOwnedDraft(Long pollId, OidcUser oidc) {
        if (oidc == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    "ポールが見つかりません。");
        }
        requireOwnedPoll(pollId, oidc);
    }

    /**
     * 二重回答を弾くメソッド
     * SINGLEはポール単位、MULTIは選択肢単位。置換しない
     * DBは呼び出し側が1回だけ引く（ここは判定のみ）
     * @param poll 対象ポール
     * @param alreadyChosen 既に選んでいる選択肢id
     * @param selected 今回選んだ選択肢
     */
    private static void validateNotAlreadyAnswered(
            Poll poll,
            List<Long> alreadyChosen,
            List<PollOption> selected
    ) {
        if (poll.getType() == PollType.SINGLE && !alreadyChosen.isEmpty()) {
            throw alreadyAnswered();
        }
        if (selected.stream()
                .anyMatch(option -> alreadyChosen.contains(option.getId()))) {
            throw alreadyAnswered();
        }
    }

    /**
     * ポールを復元するメソッド
     * 無い行は404（存在漏洩しない）
     * @param pollId ポールid
     * @return ポール
     */
    private Poll requireVisiblePoll(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "ポールが見つかりません。"));
    }

    /**
     * ポール行を悲観ロックして復元するメソッド
     * 回答POST専用。同じポールへの並行回答をTX完了まで待たせる
     * 無い行は404（存在漏洩しない）
     * @param pollId ポールid
     * @return ポール
     */
    private Poll requireLockedPoll(Long pollId) {
        return pollRepository.findByIdForUpdate(pollId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "ポールが見つかりません。"));
    }

    /**
     * 回答受付中かを検証するメソッド
     * ポールとルームが両方OPENでなければ409
     * @param poll 対象ポール
     */
    private void requireAccepting(Poll poll) {
        if (poll.getStatus() != PollStatus.OPEN
                || poll.getEvent().getStatus() != EventStatus.OPEN) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "このポールは現在回答を受け付けていません");
        }
    }

    /**
     * リクエストの選択肢idを当該ポールの選択肢へ変換するメソッド
     * 重複・件数・所属外は400
     * @param poll 対象ポール
     * @param optionIds 選んだ選択肢id
     * @return 選んだ選択肢
     */
    private List<PollOption> resolveOptions(Poll poll, List<Long> optionIds) {
        if (optionIds.stream().distinct().count() < optionIds.size()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "選択肢が重複しています。");
        }
        if (poll.getType() == PollType.SINGLE && optionIds.size() != 1) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "SINGLEは選択肢を1つ選んでください。");
        }
        if (poll.getType() == PollType.MULTI
                && (optionIds.size() < 1
                    || optionIds.size() > poll.getOptions().size())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "選択肢は1〜選択肢数にしてください。");
        }
        final Map<Long, PollOption> byId = poll.getOptions().stream()
                .collect(Collectors.toMap(PollOption::getId, option -> option));
        final List<PollOption> selected = new ArrayList<>();
        for (Long optionId : optionIds) {
            final PollOption option = byId.get(optionId);
            if (option == null) {
                throw new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        "当該ポールの選択肢を選んでください。");
            }
            selected.add(option);
        }
        return selected;
    }

    /**
     * 選択肢ごとの得票を集めるメソッド
     * SQLは1回。0票の選択肢は行が無いのでformが0埋めする
     * @param pollId ポールid
     * @return 選択肢id→得票
     */
    private Map<Long, Integer> countByOptionId(Long pollId) {
        return pollVoteRepository.countByOptionId(pollId)
                .stream()
                .collect(Collectors.toMap(
                        OptionVoteCount::optionId,
                        row -> (int) row.count()));
    }

    /**
     * 今のセッションが選んだ選択肢idを返すメソッド
     * 二重回答の判定とmyOptionIdsを兼ねる
     * @param pollId ポールid
     * @param voterKey g:{guestId}またはu:{userId}
     * @return 選んだ選択肢id
     */
    private List<Long> myOptionIds(Long pollId, String voterKey) {
        return pollVoteRepository
                .findByPoll_IdAndVoterKey(pollId, voterKey)
                .stream()
                .map(vote -> vote.getPollOption().getId())
                .toList();
    }

    /**
     * 二重回答の409を発行するメソッド
     * @return 既に回答済みの業務例外
     */
    private static BusinessException alreadyAnswered() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "Conflict",
                "既に回答済みです。");
    }

    /**
     * UNIQUE違反（同じ選択肢×同じvoter_key）かを判定するメソッド
     * メッセージに制約名が含まれる場合だけ業務409にする
     * @param ex 保存時の制約違反
     * @return ポール回答のUNIQUEならtrue
     */
    private static boolean isPollVoteUniqueViolation(DataIntegrityViolationException ex) {
        final String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(POLL_VOTE_UNIQUE);
    }

    /**
     * 公開してよいイベントを復元するメソッド
     * 無い行は404（存在漏洩しない）
     * @param eventId イベントid
     * @return Event
     */
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

    /**
     * 選択肢ラベルの前後空白を除き、重複を弾くメソッド
     * @param options リクエストのラベル
     * @return trim済みラベル
     */
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
