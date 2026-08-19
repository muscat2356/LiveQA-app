package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByQuestion_Event_IdAndVoterKey(Long eventId, String voterKey);

    boolean existsByQuestion_IdAndVoterKey(Long questionId, String voterKey);

    Optional<Vote> findByQuestion_IdAndVoterKey(Long questionId, String voterKey);

    void deleteByQuestion_IdAndVoterKey(Long questionId, String voterKey);
}
