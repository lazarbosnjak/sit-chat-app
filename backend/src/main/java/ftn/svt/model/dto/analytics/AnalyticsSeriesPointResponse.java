package ftn.svt.model.dto.analytics;

import java.time.LocalDate;

public record AnalyticsSeriesPointResponse(
        LocalDate bucketStart,
        LocalDate bucketEnd,
        long registeredUsers,
        long activeUsers,
        long exchangedMessages,
        long createdGroups
) {
}
