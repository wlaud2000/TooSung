package com.project.toosung_back.domain.feed.converter;

import com.project.toosung_back.domain.feed.dto.response.FeedResDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FeedConverter {

    // JPQL이 지원하지 않는 UNION을 EntityManager native query로 해결
    public static FeedResDTO.FeedItem toFeedItemFromRow(Object[] row) {
        return FeedResDTO.FeedItem.builder()
                .type((String) row[0])
                .itemId(((Number) row[1]).longValue())
                .stockName((String) row[2])
                .title((String) row[3])
                .publishedAt(toLocalDateTime(row[4]))
                .build();
    }

    // MySQL Connector 버전에 따라 DATETIME 컬럼이 Timestamp 또는 LocalDateTime으로 반환됨
    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        throw new IllegalStateException("Unexpected datetime type: " + value.getClass().getName());
    }
}
