package com.liveqa.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liveqa.domain.Question;
import com.liveqa.domain.Vote;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.response.VoteResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.QuestionRepository;
import com.liveqa.repository.VoteRepository;

@Service
public class VoteService {

    private final QuestionRepository questionRepository;
    private final VoteRepository voteRepository;
    private final VoterKeyResolver voterKeyResolver;
    private static final String VOTE_UNIQUE = "uq_votes_question_voter";

    public VoteService(QuestionRepository questionRepository, VoteRepository voteRepository,
            VoterKeyResolver voterKeyResolver) {
        this.questionRepository = questionRepository;
        this.voteRepository = voteRepository;
        this.voterKeyResolver = voterKeyResolver;
    }

    /**
     * いいねを追加するメソッド
     * VoterKeyをsourceから取得し、質問情報をDBより取得
     * 非OPENは409
     * @param questionId 質問id
     * @param source 投票キーの取得元
     * @return いいね後の件数と投票フラグ
     */
    @Transactional
    public VoteResponse add(Long questionId, VoterKeySource source) {
        final VoterKey voteKey = voterKeyResolver.resolve(source);
        final Question question = requireOpenQuestion(questionId);

        if (voteRepository.existsByQuestion_IdAndVoterKey(
            questionId,
            voteKey.value()
        )) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "Conflict",
                "既に投票済みです。"
            );
        }
        try {
            voteRepository.saveAndFlush(Vote.cast(question, voteKey.value()));
        } catch (DataIntegrityViolationException ex) {
            if (isVoteUniqueViolation(ex)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "Conflict",
                        "既に投票済みです。");
            }
            throw ex;
        }
        questionRepository.incrementUpvoteCount(questionId);

        return toVoteResponse(questionId, true);
    }

    /**
     * いいねを取り消すメソッド
     * @param questionId 対象の質問id
     * @param source 投票キーの取得元
     * @return 取り消された投票情報　フラグOFF
     */
    @Transactional
    public VoteResponse remove(Long questionId, VoterKeySource source) {
        final VoterKey voterKey = voterKeyResolver.resolve(source);

        requireOpenQuestion(questionId);

        if (!voteRepository.existsByQuestion_IdAndVoterKey(
            questionId,
            voterKey.value())
        ) {
           throw new BusinessException(
            HttpStatus.NOT_FOUND,
            "Not Found",
            "投票が見つかりません。"
        ); 
        }
        voteRepository.deleteByQuestion_IdAndVoterKey(
            questionId,
            voterKey.value()
        );
        questionRepository.decrementUpvoteCount(questionId);
        return toVoteResponse(questionId, false);
    }

    /**
     * OPENルームの質問を特定するメソッド
     * 見つからない場合には404 非OPENは409
     * @param questionId 質問id
     * @return 取得した質問
     */
    private Question requireOpenQuestion(Long questionId) {
        final Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    "質問が見つかりません。"
                ));
        if (question.getEvent().getStatus() != EventStatus.OPEN) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "Conflict",
                "このルームは現在質問を受け付けていません"
            );
        }

        return question;
    }

    /**
     * レスポンスDTOへ変換するメソッド
     */
    private VoteResponse toVoteResponse(Long questionId, boolean votedByMe) {
        final Question refreshed = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    "質問が見つかりません。"
                ));
        return new VoteResponse(questionId, refreshed.getUpvoteCount(), votedByMe);
    }

    /**
     * UNIQUE 違反（同じ質問×同じ voter_key）かを判定する。
     * メッセージに制約名が含まれる場合だけ業務 409 にする。
     */
    private static boolean isVoteUniqueViolation(DataIntegrityViolationException ex) {
        final String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(VOTE_UNIQUE);
    }

}
