package com.project.toosung_back.domain.news.entity;

import com.project.toosung_back.domain.news.enums.NewsCategory;
import com.project.toosung_back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "news")
public class News extends BaseEntity {

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

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "news_category", nullable = true, length = 20)
    private NewsCategory newsCategory;

    @Column(name = "region", nullable = true, length = 50)
    private String region;
}
