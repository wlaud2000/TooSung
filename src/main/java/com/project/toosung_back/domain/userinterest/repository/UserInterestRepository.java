package com.project.toosung_back.domain.userinterest.repository;

import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.userinterest.entity.UserInterest;
import com.project.toosung_back.domain.userinterest.enums.InterestType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByMemberOrderByWeightDesc(Member member);

    List<UserInterest> findByMemberAndInterestTypeOrderByWeightDesc(Member member, InterestType interestType);

    Optional<UserInterest> findByMemberAndInterestTypeAndTopic(Member member, InterestType interestType, String topic);

    boolean existsByMemberAndInterestTypeAndTopic(Member member, InterestType interestType, String topic);

    List<UserInterest> findByMember_IdOrderByWeightDesc(Long memberId, Pageable pageable);
}
