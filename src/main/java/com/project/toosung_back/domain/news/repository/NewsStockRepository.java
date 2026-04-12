package com.project.toosung_back.domain.news.repository;

import com.project.toosung_back.domain.news.entity.NewsStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsStockRepository extends JpaRepository<NewsStock, Long> {

    @Query("SELECT ns FROM NewsStock ns JOIN FETCH ns.stock WHERE ns.news.id IN :newsIds")
    List<NewsStock> findAllByNewsIdIn(@Param("newsIds") List<Long> newsIds);
}
