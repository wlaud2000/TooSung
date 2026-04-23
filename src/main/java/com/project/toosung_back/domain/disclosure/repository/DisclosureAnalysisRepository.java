package com.project.toosung_back.domain.disclosure.repository;

import com.project.toosung_back.domain.disclosure.entity.DisclosureAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DisclosureAnalysisRepository extends JpaRepository<DisclosureAnalysis, Long> {

    boolean existsByDisclosureId(Long disclosureId);

    Optional<DisclosureAnalysis> findByDisclosureId(Long disclosureId);

    @Query("SELECT da FROM DisclosureAnalysis da " +
            "JOIN FETCH da.disclosure d " +
            "JOIN FETCH d.stock " +
            "WHERE d.stock.id IN :stockIds " +
            "AND d.publishedAt >= :from " +
            "ORDER BY d.publishedAt DESC")
    List<DisclosureAnalysis> findTodayByStockIds(@Param("stockIds") List<Long> stockIds,
                                                 @Param("from") LocalDateTime from,
                                                 Pageable pageable);
}