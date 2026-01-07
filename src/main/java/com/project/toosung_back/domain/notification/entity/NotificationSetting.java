package com.project.toosung_back.domain.notification.entity;

import com.project.toosung_back.domain.member.entity.Member;
import com.project.toosung_back.global.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "notification_setting")
public class NotificationSetting extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "push_breaking_news", nullable = false)
    private Boolean pushBreakingNews = true;

    @Builder.Default
    @Column(name = "push_disclosure", nullable = false)
    private Boolean pushDisclosure = false;

    @Builder.Default
    @Column(name = "email_briefing", nullable = false)
    private Boolean emailBriefing = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public void updateSettings(Boolean pushBreakingNews, Boolean pushDisclosure, Boolean emailBriefing) {
        this.pushBreakingNews = pushBreakingNews;
        this.pushDisclosure = pushDisclosure;
        this.emailBriefing = emailBriefing;
    }
}
