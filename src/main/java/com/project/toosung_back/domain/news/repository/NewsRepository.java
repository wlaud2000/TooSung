package com.project.toosung_back.domain.news.repository;

import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.enums.Sentiment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {

    boolean existsByUrl(String url);

    @Query("SELECT n FROM News n " +
            "JOIN NewsStock ns ON ns.news = n " +
            "JOIN NewsAnalysis na ON na.news = n " +
            "WHERE ns.stock.id = :stockId " +
            "AND na.isRelevant = true " +
            "AND (:cursor IS NULL OR n.id < :cursor) " +
            "ORDER BY n.id DESC")
    Slice<News> findByStockIdWithCursor(@Param("stockId") Long stockId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT n FROM News n " +
            "JOIN NewsStock ns ON ns.news = n " +
            "JOIN NewsAnalysis na ON na.news.id = n.id " +
            "WHERE ns.stock.id = :stockId " +
            "AND na.isRelevant = true " +
            "AND na.sentiment = :sentiment " +
            "AND (:cursor IS NULL OR n.id < :cursor) " +
            "ORDER BY n.id DESC")
    Slice<News> findByStockIdAndSentimentWithCursor(@Param("stockId") Long stockId, @Param("sentiment") Sentiment sentiment, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT n FROM News n " +
            "WHERE NOT EXISTS (SELECT 1 FROM NewsAnalysis na WHERE na.news.id = n.id) " +
            "ORDER BY n.id DESC")
    List<News> findUnanalyzedNews(Pageable pageable);
}
