package com.project.toosung_back.domain.news.entity;

import com.project.toosung_back.domain.news.enums.Sentiment;
import com.project.toosung_back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "news_analysis")
public class NewsAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false, unique = true)
    private News news;

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", nullable = false, length = 20)
    private Sentiment sentiment;

    @Column(name = "sentiment_reason", columnDefinition = "TEXT")
    private String sentimentReason;

    @Column(name = "is_relevant", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isRelevant;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
}
