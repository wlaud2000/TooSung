package com.project.toosung_back.domain.disclosure.entity;

import com.project.toosung_back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "disclosure_analysis")
public class DisclosureAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disclosure_id", nullable = false, unique = true)
    private Disclosure disclosure;

    @Column(name = "simple_summary", columnDefinition = "TEXT", nullable = false)
    private String simpleSummary;

    @Column(name = "investment_point", columnDefinition = "TEXT")
    private String investmentPoint;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
}
