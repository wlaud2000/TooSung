package com.project.toosung_back.domain.disclosure.entity;

import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "disclosure")
public class Disclosure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dart_id", nullable = false)
    private String dartId;

    @Column(name = "disclosure_type", nullable = false, length = 50)
    private String disclosureType;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_impact", columnDefinition = "TEXT")
    private String aiImpact;

    @Column(name = "ai_investment_point", columnDefinition = "TEXT")
    private String aiInvestmentPoint;

    @Column(name = "ai_analyzed_at")
    private LocalDateTime aiAnalyzedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
}
