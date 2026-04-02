package com.project.toosung_back.domain.news.service.query;

import com.project.toosung_back.domain.news.converter.NewsConverter;
import com.project.toosung_back.domain.news.dto.response.NewsResDTO;
import com.project.toosung_back.domain.news.entity.News;
import com.project.toosung_back.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsQueryService {

    private final NewsRepository newsRepository;

    public NewsResDTO.NewsList getNews(Long stockId, Long cursor, int size) {
        Slice<News> slice = newsRepository.findByStockIdWithCursor(stockId, cursor, PageRequest.of(0, size));
        List<NewsResDTO.NewsItem> items = slice.getContent().stream()
                .map(NewsConverter::toNewsItem)
                .toList();

        // 마지막 item의 id를 nextCursor로 설정
        Long nextCursor = slice.hasNext() ? slice.getContent().get(slice.getContent().size() -1).getId() : null;

        return NewsResDTO.NewsList.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(slice.hasNext())
                .build();
    }
}
