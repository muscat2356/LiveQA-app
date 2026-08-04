package com.liveqa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.User;


public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByOidcSubject(String oidcSubject);
}
