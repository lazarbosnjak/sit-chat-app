package ftn.svt.service;

import ftn.svt.exception.ApiException;
import ftn.svt.model.ChatType;
import ftn.svt.model.dto.analytics.AnalyticsGranularity;
import ftn.svt.model.dto.analytics.AnalyticsSeriesPointResponse;
import ftn.svt.model.dto.analytics.AnalyticsTopGroupResponse;
import ftn.svt.model.dto.analytics.AnalyticsTopUserResponse;
import ftn.svt.model.dto.analytics.SystemAnalyticsResponse;
import ftn.svt.repository.ChatRepository;
import ftn.svt.repository.MessageRepository;
import ftn.svt.repository.UserActivityRepository;
import ftn.svt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final int MAX_TOP_LIMIT = 100;

    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;

    @Transactional(readOnly = true)
    public SystemAnalyticsResponse getSystemAnalytics(
            LocalDate from,
            LocalDate to,
            AnalyticsGranularity granularity,
            int topLimit
    ) {
        validateRange(from, to);
        validateTopLimit(topLimit);

        Instant rangeStart = startOfDay(from);
        Instant rangeEnd = startOfDay(to.plusDays(1));
        String bucket = granularity.getPostgresBucket();

        long totalRegisteredUsers = userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                rangeStart,
                rangeEnd
        );
        long totalActiveUsers = userActivityRepository.countActiveUsers(rangeStart, rangeEnd);
        long totalExchangedMessages = messageRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                rangeStart,
                rangeEnd
        );
        long totalCreatedGroups = chatRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                ChatType.GROUP,
                rangeStart,
                rangeEnd
        );

        List<AnalyticsSeriesPointResponse> series = buildSeries(
                from,
                to,
                granularity,
                toBucketCounts(userRepository.countRegistrationsByBucket(rangeStart, rangeEnd, bucket)),
                toBucketCounts(userActivityRepository.countActiveUsersByBucket(rangeStart, rangeEnd, bucket)),
                toBucketCounts(messageRepository.countMessagesByBucket(rangeStart, rangeEnd, bucket)),
                toBucketCounts(chatRepository.countCreatedChatsByTypeByBucket(
                        ChatType.GROUP.name(),
                        rangeStart,
                        rangeEnd,
                        bucket
                ))
        );

        PageRequest topPage = PageRequest.of(0, topLimit);

        return new SystemAnalyticsResponse(
                from,
                to,
                granularity,
                totalRegisteredUsers,
                totalActiveUsers,
                totalExchangedMessages,
                totalCreatedGroups,
                series,
                toTopUsers(messageRepository.findTopMessageSenders(rangeStart, rangeEnd, topPage)),
                toTopGroups(messageRepository.findTopChatsByMessageCount(
                        ChatType.GROUP,
                        rangeStart,
                        rangeEnd,
                        topPage
                ))
        );
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ApiException.badRequest("from and to are required");
        }

        if (from.isAfter(to)) {
            throw ApiException.badRequest("from must be before or equal to to");
        }
    }

    private void validateTopLimit(int topLimit) {
        if (topLimit < 1 || topLimit > MAX_TOP_LIMIT) {
            throw ApiException.badRequest("topLimit must be between 1 and " + MAX_TOP_LIMIT);
        }
    }

    private List<AnalyticsSeriesPointResponse> buildSeries(
            LocalDate from,
            LocalDate to,
            AnalyticsGranularity granularity,
            Map<LocalDate, Long> registeredUsers,
            Map<LocalDate, Long> activeUsers,
            Map<LocalDate, Long> exchangedMessages,
            Map<LocalDate, Long> createdGroups
    ) {
        LocalDate firstBucketStart = granularity.bucketStart(from);
        Map<LocalDate, AnalyticsSeriesPointResponse> points = new LinkedHashMap<>();

        for (
                LocalDate bucketStart = firstBucketStart;
                !bucketStart.isAfter(to);
                bucketStart = granularity.nextBucketStart(bucketStart)
        ) {
            LocalDate nextBucketStart = granularity.nextBucketStart(bucketStart);
            LocalDate bucketEnd = nextBucketStart.minusDays(1);

            if (bucketEnd.isAfter(to)) {
                bucketEnd = to;
            }

            points.put(bucketStart, new AnalyticsSeriesPointResponse(
                    bucketStart,
                    bucketEnd,
                    registeredUsers.getOrDefault(bucketStart, 0L),
                    activeUsers.getOrDefault(bucketStart, 0L),
                    exchangedMessages.getOrDefault(bucketStart, 0L),
                    createdGroups.getOrDefault(bucketStart, 0L)
            ));
        }

        return List.copyOf(points.values());
    }

    private Map<LocalDate, Long> toBucketCounts(List<Object[]> rows) {
        Map<LocalDate, Long> counts = new HashMap<>();

        rows.forEach(row -> counts.merge(
                toLocalDate(row[0]),
                toLong(row[1]),
                Long::sum
        ));

        return counts;
    }

    private List<AnalyticsTopUserResponse> toTopUsers(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new AnalyticsTopUserResponse(
                        (UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        toLong(row[5])
                ))
                .toList();
    }

    private List<AnalyticsTopGroupResponse> toTopGroups(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new AnalyticsTopGroupResponse(
                        (UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        toLong(row[3])
                ))
                .toList();
    }

    private long toLong(Object value) {
        return ((Number) value).longValue();
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }

        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDate();
        }

        if (value instanceof Instant instant) {
            return instant.atZone(ZoneOffset.UTC).toLocalDate();
        }

        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        throw ApiException.badRequest("Unsupported analytics bucket value");
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
