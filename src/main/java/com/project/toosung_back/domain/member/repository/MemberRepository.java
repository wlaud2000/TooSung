package com.project.toosung_back.domain.member.repository;

import com.project.toosung_back.domain.auth.enums.Provider;
import com.project.toosung_back.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
}
