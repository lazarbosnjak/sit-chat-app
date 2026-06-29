package ftn.svt.model.dto.analytics;

import java.util.UUID;

public record AnalyticsTopGroupResponse(
        UUID id,
        String name,
        String imageUrl,
        long messageCount
) {
}
