package com.project.toosung_back.domain.watchlist.repository;

import com.project.toosung_back.domain.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByMember_IdOrderByPositionAsc(Long memberId);
}
