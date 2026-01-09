package com.project.toosung_back.domain.auth.repository;

import com.project.toosung_back.domain.auth.entity.SocialAuth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {
}
