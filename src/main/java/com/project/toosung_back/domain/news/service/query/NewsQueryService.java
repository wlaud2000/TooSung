package com.project.toosung_back.domain.news.service.query;

import com.project.toosung_back.domain.news.converter.NewsConverter;
import com.project.toosung_back.domain.news.dto.response.NewsResDTO;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.entity.NewsAnalysis;
import com.project.toosung_back.domain.news.exception.NewsErrorCode;
import com.project.toosung_back.domain.news.exception.NewsException;
import com.project.toosung_back.domain.news.repository.NewsAnalysisRepository;
import com.project.toosung_back.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsQueryService {

    private final NewsRepository newsRepository;
    private final NewsAnalysisRepository newsAnalysisRepository;

    @Transactional(readOnly = true)
    public NewsResDTO.NewsDetail getNewsDetail(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsException(NewsErrorCode.NEWS_NOT_FOUND));

        Optional<NewsAnalysis> analysis = newsAnalysisRepository.findByNewsId(newsId);

        return NewsConverter.toNewsDetail(news, analysis);
    }

    @Transactional(readOnly = true)
    public NewsResDTO.NewsList getNews(Long stockId, Long cursor, int size) {
        Slice<News> slice = newsRepository.findByStockIdWithCursor(stockId, cursor, PageRequest.of(0, size));

        List<Long> newsIds = slice.getContent().stream()
                .map(News::getId)
                .toList();

        Map<Long, NewsAnalysis> analysisMap = newsAnalysisRepository.findAllByNewsIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getNews().getId(), a -> a));

        List<NewsResDTO.NewsItem> items = slice.getContent().stream()
                .map(news -> NewsConverter.toNewsItem(news, analysisMap.get(news.getId())))
                .toList();

        Long nextCursor = slice.hasNext()
                ? slice.getContent().get(slice.getContent().size() - 1).getId()
                : null;

        return NewsResDTO.NewsList.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(slice.hasNext())
                .build();
    }
}
