package com.staysphere.analyticsservice.controller;
import com.staysphere.analyticsservice.service.AnalyticsService;
import com.staysphere.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController @RequestMapping("/api/v1/analytics") @RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    /** Admin-only: full platform dashboard */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsService.AdminDashboard>> adminDashboard(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().minusDays(30).toString()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAdminDashboard(from, to)));
    }

    /** Host: their own property dashboard */
    @GetMapping("/host/dashboard")
    @PreAuthorize("hasAnyRole('HOST','ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsService.HostDashboard>> hostDashboard(
            @AuthenticationPrincipal String hostId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().minusDays(30).toString()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getHostDashboard(hostId, from, to)));
    }

    /** Record frontend page view event (called from theme JS) */
    @PostMapping("/pageview")
    public ResponseEntity<ApiResponse<Void>> recordPageView(@RequestBody Map<String,String> req) {
        analyticsService.recordPageView(req.get("sessionId"), req.get("userId"),
                req.get("pageType"), req.get("pageId"), req.get("pageTitle"),
                req.get("referrer"), req.get("userAgent"), req.get("ipAddress"),
                req.get("country"), req.get("deviceType"));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** Admin: get site performance metrics */
    @GetMapping("/admin/site-performance")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<Object>> sitePerformance(
            @RequestParam(defaultValue = "30") int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getAdminDashboard(from, to)));
    }
}
