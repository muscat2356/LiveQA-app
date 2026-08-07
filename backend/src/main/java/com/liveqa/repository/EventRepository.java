package com.liveqa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.Event;
import java.util.List;


public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByJoinCode(String joinCode);

    List<Event> findByHost_IdOrderByCreatedAtDesc(Long hostId);

}
