package com.project.toosung_back.domain.news.repository;

import com.project.toosung_back.domain.news.entity.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsRepository extends JpaRepository<News, Long> {
    boolean existsByUrl(String url);

    @Query("SELECT n FROM News n " +
            "JOIN NewsStock ns ON ns.news = n " +
            "WHERE ns.stock.id = :stockId " +
            "AND (:cursor IS NULL OR n.id < :cursor) " +
            "ORDER BY n.id DESC")
    Slice<News> findByStockIdWithCursor(@Param("stockId") Long stockId, @Param("cursor") Long cursor, Pageable pageable);
}
