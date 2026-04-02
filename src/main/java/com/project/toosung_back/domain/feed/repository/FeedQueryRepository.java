package com.project.toosung_back.domain.feed.repository;

import com.project.toosung_back.domain.feed.converter.FeedConverter;
import com.project.toosung_back.domain.feed.dto.response.FeedResDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FeedQueryRepository {

    private final EntityManager em;

    private static final String FEED_SQL = """
            SELECT 'NEWS' AS type, n.id AS item_id, MIN(s.name) AS stock_name, n.title AS title, n.published_at AS published_at
            FROM news n
            JOIN news_stock ns ON ns.news_id = n.id
            JOIN watchlist w ON w.stock_id = ns.stock_id
            JOIN stock s ON s.id = ns.stock_id
            WHERE w.member_id = :memberId
              AND w.deleted_at IS NULL
            GROUP BY n.id, n.title, n.published_at
            UNION ALL
            SELECT 'DISCLOSURE' AS type, d.id AS item_id, s.name AS stock_name, d.disclosure_type AS title, d.published_at AS published_at
            FROM disclosure d
            JOIN watchlist w ON w.stock_id = d.stock_id
            JOIN stock s ON s.id = d.stock_id
            WHERE w.member_id = :memberId
              AND w.deleted_at IS NULL
            ORDER BY published_at DESC
            """;

    private static final String FEED_SQL_WITH_CURSOR = """
            SELECT 'NEWS' AS type, n.id AS item_id, MIN(s.name) AS stock_name, n.title AS title, n.published_at AS published_at
            FROM news n
            JOIN news_stock ns ON ns.news_id = n.id
            JOIN watchlist w ON w.stock_id = ns.stock_id
            JOIN stock s ON s.id = ns.stock_id
            WHERE w.member_id = :memberId
              AND w.deleted_at IS NULL
              AND n.published_at < :cursor
            GROUP BY n.id, n.title, n.published_at
            UNION ALL
            SELECT 'DISCLOSURE' AS type, d.id AS item_id, s.name AS stock_name, d.disclosure_type AS title, d.published_at AS published_at
            FROM disclosure d
            JOIN watchlist w ON w.stock_id = d.stock_id
            JOIN stock s ON s.id = d.stock_id
            WHERE w.member_id = :memberId
              AND w.deleted_at IS NULL
              AND d.published_at < :cursor
            ORDER BY published_at DESC
            """;

    @SuppressWarnings("unchecked")
    public List<FeedResDTO.FeedItem> findFeed(Long memberId, LocalDateTime cursor, int limit) {
        String sql = cursor != null ? FEED_SQL_WITH_CURSOR : FEED_SQL;
        Query query = em.createNativeQuery(sql);
        query.setParameter("memberId", memberId);
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }
        query.setMaxResults(limit);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(FeedConverter::toFeedItemFromRow)
                .toList();
    }
}