package ftn.svt.model.dto.analytics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Locale;

public enum AnalyticsGranularity {
    DAILY("day"),
    WEEKLY("week"),
    MONTHLY("month"),
    YEARLY("year");

    private final String postgresBucket;

    AnalyticsGranularity(String postgresBucket) {
        this.postgresBucket = postgresBucket;
    }

    public String getPostgresBucket() {
        return postgresBucket;
    }

    public static AnalyticsGranularity fromRequestValue(String value) {
        if (value == null || value.isBlank()) {
            return DAILY;
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(granularity -> granularity.name().equals(normalizedValue)
                        || granularity.postgresBucket.equals(value.trim().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported analytics granularity"));
    }

    public LocalDate bucketStart(LocalDate date) {
        return switch (this) {
            case DAILY -> date;
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> date.withDayOfMonth(1);
            case YEARLY -> date.withDayOfYear(1);
        };
    }

    public LocalDate nextBucketStart(LocalDate date) {
        return switch (this) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }
}
