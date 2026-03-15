package com.project.toosung_back.domain.userinterest.entity;

import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.domain.userinterest.enums.InterestType;
import com.project.toosung_back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "user_interest",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_interest_member_type_topic",
                columnNames = {"member_id", "interest_type", "topic"}
        )
)
public class UserInterest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 20)
    private InterestType interestType;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Builder.Default
    @Column(name = "weight", nullable = false)
    private Double weight = 1.0;

    @Column(name = "last_interaction", nullable = false)
    private LocalDateTime lastInteraction;

    public void increaseWeight(double delta) {
        this.weight = Math.min(this.weight + delta, 1.0);
        this.lastInteraction = LocalDateTime.now();
    }

    public void decreaseWeight(double delta) {
        this.weight = Math.max(this.weight - delta, 0.0);
    }

    public void updateLastInteraction() {
        this.lastInteraction = LocalDateTime.now();
    }
}
