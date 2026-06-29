package ftn.svt.model.dto.analytics;

import java.time.LocalDate;
import java.util.List;

public record SystemAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        AnalyticsGranularity granularity,
        long totalRegisteredUsers,
        long totalActiveUsers,
        long totalExchangedMessages,
        long totalCreatedGroups,
        List<AnalyticsSeriesPointResponse> series,
        List<AnalyticsTopUserResponse> topUsers,
        List<AnalyticsTopGroupResponse> topGroups
) {
}
