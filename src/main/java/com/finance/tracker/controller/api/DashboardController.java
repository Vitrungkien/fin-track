package com.finance.tracker.controller.api;

import com.finance.tracker.dto.response.ChartDataResponse;
import com.finance.tracker.dto.response.DashboardSummaryResponse;
import com.finance.tracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getSummary(month, year));
    }

    @GetMapping("/chart/category")
    public ResponseEntity<ChartDataResponse> getCategoryChart(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getCategoryChartData(month, year));
    }

    @GetMapping("/chart/daily")
    public ResponseEntity<ChartDataResponse> getDailyChart(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getDailyChartData(month, year));
    }
}
