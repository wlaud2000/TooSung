package com.project.toosung_back.domain.news.repository;

import com.project.toosung_back.domain.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {
    boolean existsByUrl(String url);
}
