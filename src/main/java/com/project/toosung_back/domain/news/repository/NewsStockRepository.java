package com.project.toosung_back.domain.news.repository;

import com.project.toosung_back.domain.news.entity.NewsStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsStockRepository extends JpaRepository<NewsStock, Long> {
}
