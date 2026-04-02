package com.project.toosung_back.domain.feed.service;

import com.project.toosung_back.domain.feed.dto.response.FeedResDTO;
import com.project.toosung_back.domain.feed.repository.FeedQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedQueryService {

    private final FeedQueryRepository feedQueryRepository;

    @Transactional(readOnly = true)
    public FeedResDTO.FeedList getFeed(Long memberId, String cursor, int size) {
        LocalDateTime cursorTime = cursor != null ? LocalDateTime.parse(cursor) : null;
        List<FeedResDTO.FeedItem> fetched = feedQueryRepository.findFeed(memberId, cursorTime, size + 1);

        boolean hasNext = fetched.size() > size;
        List<FeedResDTO.FeedItem> items = hasNext ? fetched.subList(0, size) : fetched;
        String nextCursor = hasNext ? items.get(items.size() - 1).publishedAt().toString() : null;

        return FeedResDTO.FeedList.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}