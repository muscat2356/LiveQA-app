package com.liveqa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByEvent_IdOrderByCreatedAtDesc(Long eventId);
}
