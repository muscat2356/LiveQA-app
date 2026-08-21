package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.liveqa.domain.Event;
import com.liveqa.domain.Question;
import com.liveqa.domain.User;
import com.liveqa.domain.Vote;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.response.VoteResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.QuestionRepository;
import com.liveqa.repository.VoteRepository;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    QuestionRepository questionRepository;

    @Mock
    VoteRepository voteRepository;

    @Mock
    VoterKeyResolver voterKeyResolver;

    @InjectMocks
    VoteService voteService;

    @Test
    void add_未投票ならINSERTして件数を足す() {
        final Question question = openQuestion(42L);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(questionRepository.findById(42L)).thenReturn(Optional.of(question));
        when(voteRepository.existsByQuestion_IdAndVoterKey(42L, "g:abc")).thenReturn(false);
        when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.incrementUpvoteCount(42L)).thenAnswer(inv -> {
            ReflectionTestUtils.setField(question, "upvoteCount", 1);
            return 1;
        });

        final VoteResponse response = voteService.add(42L, new VoterKeySource.Guest("abc"));

        assertThat(response.questionId()).isEqualTo(42L);
        assertThat(response.upvoteCount()).isEqualTo(1);
        assertThat(response.votedByMe()).isTrue();
        verify(voteRepository).saveAndFlush(any(Vote.class));
        verify(questionRepository).incrementUpvoteCount(42L);
    }

    @Test
    void add_二重なら409() {
        final Question question = openQuestion(42L);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(questionRepository.findById(42L)).thenReturn(Optional.of(question));
        when(voteRepository.existsByQuestion_IdAndVoterKey(42L, "g:abc")).thenReturn(true);

        assertThatThrownBy(() -> voteService.add(42L, new VoterKeySource.Guest("abc")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("既に投票済み");
                });
        verify(voteRepository, never()).saveAndFlush(any());
        verify(questionRepository, never()).incrementUpvoteCount(any());
    }

    @Test
    void add_UNIQUE違反なら409() {
        final Question question = openQuestion(42L);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(questionRepository.findById(42L)).thenReturn(Optional.of(question));
        when(voteRepository.existsByQuestion_IdAndVoterKey(42L, "g:abc")).thenReturn(false);
        when(voteRepository.saveAndFlush(any(Vote.class)))
                .thenThrow(new DataIntegrityViolationException("uq_votes_question_voter"));

        assertThatThrownBy(() -> voteService.add(42L, new VoterKeySource.Guest("abc")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("既に投票済み");
                });
        verify(questionRepository, never()).incrementUpvoteCount(any());
    }

    @Test
    void add_Missingなら401() {
        when(voterKeyResolver.resolve(any())).thenThrow(new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "匿名セッションが未確立です。"));

        assertThatThrownBy(() -> voteService.add(42L, new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
        verify(questionRepository, never()).findById(any());
        verify(voteRepository, never()).saveAndFlush(any());
    }

    @Test
    void add_非OPENなら409() {
        final Question question = draftQuestion(42L);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(questionRepository.findById(42L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> voteService.add(42L, new VoterKeySource.Guest("abc")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("受け付けていません");
                });
        verify(voteRepository, never()).saveAndFlush(any());
        verify(questionRepository, never()).incrementUpvoteCount(any());
    }

    @Test
    void add_質問無しなら404() {
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.add(99L, new VoterKeySource.Guest("abc")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(voteRepository, never()).saveAndFlush(any());
    }

    @Test
    void remove_票があればDELETEして件数を減らす() {
        final Question question = openQuestion(42L);
        ReflectionTestUtils.setField(question, "upvoteCount", 1);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(questionRepository.findById(42L)).thenReturn(Optional.of(question));
        when(voteRepository.existsByQuestion_IdAndVoterKey(42L, "g:abc")).thenReturn(true);
        when(questionRepository.decrementUpvoteCount(42L)).thenAnswer(inv -> {
            ReflectionTestUtils.setField(question, "upvoteCount", 0);
            return 1;
        });

        final VoteResponse response = voteService.remove(42L, new VoterKeySource.Guest("abc"));

        assertThat(response.questionId()).isEqualTo(42L);
        assertThat(response.upvoteCount()).isZero();
        assertThat(response.votedByMe()).isFalse();
        verify(voteRepository).deleteByQuestion_IdAndVoterKey(42L, "g:abc");
        verify(questionRepository).decrementUpvoteCount(42L);
    }

    @Test
    void remove_票が無ければ404() {
        final Question question = openQuestion(42L);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(questionRepository.findById(42L)).thenReturn(Optional.of(question));
        when(voteRepository.existsByQuestion_IdAndVoterKey(42L, "g:abc")).thenReturn(false);

        assertThatThrownBy(() -> voteService.remove(42L, new VoterKeySource.Guest("abc")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getMessage()).contains("投票が見つかりません");
                });
        verify(voteRepository, never()).deleteByQuestion_IdAndVoterKey(any(), any());
        verify(questionRepository, never()).decrementUpvoteCount(any());
    }

    private Question openQuestion(Long id) {
        final Event event = openEvent(10L);
        final Question question = Question.post(event, "資料は？", "花子");
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private Question draftQuestion(Long id) {
        final User host = User.hostOf("sub-1");
        ReflectionTestUtils.setField(host, "id", 1L);
        final Event event = Event.create(host, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        final Question question = Question.post(event, "資料は？", "花子");
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private Event openEvent(Long id) {
        final User host = User.hostOf("sub-1");
        ReflectionTestUtils.setField(host, "id", 1L);
        final Event event = Event.create(host, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", id);
        event.changeStatus(EventStatus.OPEN);
        return event;
    }
}
