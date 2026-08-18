package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.Event;


public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByJoinCode(String joinCode);

    List<Event> findByHost_IdOrderByCreatedAtDesc(Long hostId);

    Optional<Event> findByJoinCode(String joinCode);

    Optional<Event> findByIdAndHost_Id(Long id, Long hostId);
}
