package com.project.toosung_back.domain.news.entity;

import com.project.toosung_back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "real_estate_news")
public class RealEstateNews extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "region", nullable = false, length = 50)
    private String region;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;
}
