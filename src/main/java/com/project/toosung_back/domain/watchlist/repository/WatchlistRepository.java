package com.project.toosung_back.domain.watchlist.repository;

import com.project.toosung_back.domain.stock.entity.Stock;
import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByMember_IdOrderByPositionAsc(Long memberId);

    boolean existsByMember_IdAndStock_IdAndDeletedAtIsNull(Long memberId, Long stockId);

    long countByMember_IdAndDeletedAtIsNull(Long memberId);

    // 삭제되지 않은 관심 종목에서 중복 없이 Stock 조회
    @Query("SELECT DISTINCT w.stock FROM Watchlist w WHERE w.deletedAt IS NULL")
    List<Stock> findAllDistinctStocks();
}
