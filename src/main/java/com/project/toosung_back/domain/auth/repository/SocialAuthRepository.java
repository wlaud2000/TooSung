package com.project.toosung_back.domain.auth.repository;

import com.project.toosung_back.domain.auth.entity.SocialAuth;
import com.project.toosung_back.domain.auth.enums.Provider;
import com.project.toosung_back.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {
    Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);

    @Query("SELECT s.member FROM SocialAuth s WHERE s.provider = :provider AND s.providerId = :providerId")
    Optional<Member> findMemberByProviderAndProviderId(@Param("provider") Provider provider,
                                                       @Param("providerId") String providerId);
}
