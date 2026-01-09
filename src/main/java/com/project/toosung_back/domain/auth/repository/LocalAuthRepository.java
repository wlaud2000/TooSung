package com.project.toosung_back.domain.auth.repository;

import com.project.toosung_back.domain.auth.entity.LocalAuth;
import com.project.toosung_back.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalAuthRepository extends JpaRepository<LocalAuth, Long> {
    boolean existsByMember(Member member);
    Optional<LocalAuth> findByMember(Member member);
}
