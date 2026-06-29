package ftn.svt.model.dto.analytics;

import java.util.UUID;

public record AnalyticsTopUserResponse(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String pfpUrl,
        long messageCount
) {
}
