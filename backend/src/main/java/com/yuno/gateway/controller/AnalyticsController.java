package com.yuno.gateway.controller;

import com.yuno.gateway.dto.response.DashboardResponse;
import com.yuno.gateway.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics and reporting APIs")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary",
               description = "Aggregated metrics including volume, success rates, provider performance")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(defaultValue = "24") int hoursBack) {
        return ResponseEntity.ok(analyticsService.getDashboard(hoursBack));
    }
}
