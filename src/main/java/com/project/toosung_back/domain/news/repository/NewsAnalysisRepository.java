package com.project.toosung_back.domain.news.repository;

import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NewsAnalysisRepository extends JpaRepository<NewsAnalysis, Long> {

    boolean existsByNewsId(Long newsId);

    Optional<NewsAnalysis> findByNewsId(Long newsId);

    @Query("SELECT na FROM NewsAnalysis na WHERE na.news.id IN :newsIds")
    List<NewsAnalysis> findAllByNewsIdIn(@Param("newsIds") Collection<Long> newsIds);
}
