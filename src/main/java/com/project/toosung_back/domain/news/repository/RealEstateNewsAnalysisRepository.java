package com.project.toosung_back.domain.news.repository;

import com.project.toosung_back.domain.news.entity.RealEstateNewsAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RealEstateNewsAnalysisRepository extends JpaRepository<RealEstateNewsAnalysis, Long> {

    @Query("SELECT na FROM RealEstateNewsAnalysis na WHERE na.realEstateNews.id IN :newsIds")
    List<RealEstateNewsAnalysis> findAllByRealEstateNewsIdIn(@Param("newsIds") Collection<Long> newsIds);
}
