package com.project.toosung_back.domain.disclosure.repository;

import com.project.toosung_back.domain.disclosure.entity.Disclosure;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DisclosureRepository extends JpaRepository<Disclosure, Long> {

    boolean existsByDartId(String dartId);

    long countBySource(com.project.toosung_back.domain.disclosure.entity.DisclosureSource source);

    @Query("SELECT d FROM Disclosure  d JOIN FETCH d.stock WHERE d.id = :id")
    Optional<Disclosure> findByIdWithStock(@Param("id") Long id);

    @Query("SELECT d FROM Disclosure d JOIN FETCH d.stock " +
            "WHERE d.stock.id = :stockId " +
            "AND (:cursor IS NULL OR d.id < :cursor) " +
            "AND (:type IS NULL OR d.disclosureType LIKE CONCAT('%', :type, '%')) " +
            "ORDER BY d.id DESC")
    Slice<Disclosure> findDisclosures(@Param("stockId") Long stockId,
                                      @Param("cursor") Long cursor,
                                      @Param("type") String type,
                                      Pageable pageable);

    @Query("SELECT d FROM Disclosure d JOIN FETCH d.stock " +
            "WHERE NOT EXISTS (SELECT 1 FROM DisclosureAnalysis da WHERE da.disclosure.id = d.id) " +
            "ORDER BY d.id DESC")
    List<Disclosure> findUnanalyzedDisclosures(Pageable pageable);

    @Query("SELECT d.stock.id, COUNT(d) " +
            "FROM Disclosure d " +
            "WHERE d.stock.id IN :stockIds " +
            "AND d.publishedAt >= :from " +
            "GROUP BY d.stock.id")
    List<Object[]> countByStockIds(@Param("stockIds") List<Long> stockIds,
                                   @Param("from") java.time.LocalDateTime from);

    boolean existsByStock_Id(Long stockId);

    boolean existsByStock_IdAndSource(Long stockId, com.project.toosung_back.domain.disclosure.entity.DisclosureSource source);
}
