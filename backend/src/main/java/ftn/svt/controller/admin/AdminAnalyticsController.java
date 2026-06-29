package ftn.svt.controller.admin;

import ftn.svt.exception.ApiException;
import ftn.svt.model.dto.analytics.AnalyticsGranularity;
import ftn.svt.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v0/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<?> getSystemAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "DAILY") String granularity,
            @RequestParam(required = false, defaultValue = "10") int topLimit
    ) {
        try {
            return ResponseEntity.ok(analyticsService.getSystemAnalytics(
                    from,
                    to,
                    AnalyticsGranularity.fromRequestValue(granularity),
                    topLimit
            ));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(ex.getMessage());
        }
    }
}
