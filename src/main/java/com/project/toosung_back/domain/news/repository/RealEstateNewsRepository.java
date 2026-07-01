package com.project.toosung_back.domain.news.repository;

import com.project.toosung_back.domain.news.entity.RealEstateNews;
import com.project.toosung_back.domain.news.enums.Sentiment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RealEstateNewsRepository extends JpaRepository<RealEstateNews, Long> {

    @Query("SELECT n.url FROM RealEstateNews n WHERE n.url IN :urls")
    List<String> findExistingUrls(@Param("urls") List<String> urls);

    @Query("SELECT n FROM RealEstateNews n " +
            "WHERE NOT EXISTS (SELECT 1 FROM RealEstateNewsAnalysis na WHERE na.realEstateNews.id = n.id) " +
            "ORDER BY n.id DESC")
    List<RealEstateNews> findUnanalyzed(Pageable pageable);

    @Query("SELECT n FROM RealEstateNews n " +
            "JOIN RealEstateNewsAnalysis na ON na.realEstateNews = n " +
            "WHERE n.region = :region " +
            "AND na.isRelevant = true " +
            "AND (:cursor IS NULL OR n.publishedAt < :cursor) " +
            "ORDER BY n.publishedAt DESC")
    Slice<RealEstateNews> findByRegionWithCursor(
            @Param("region") String region,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);

    @Query("SELECT n FROM RealEstateNews n " +
            "JOIN RealEstateNewsAnalysis na ON na.realEstateNews = n " +
            "WHERE n.region = :region " +
            "AND na.sentiment = :sentiment " +
            "AND na.isRelevant = true " +
            "AND (:cursor IS NULL OR n.publishedAt < :cursor) " +
            "ORDER BY n.publishedAt DESC")
    Slice<RealEstateNews> findByRegionAndSentimentWithCursor(
            @Param("region") String region,
            @Param("sentiment") Sentiment sentiment,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);
}
