package tmmsystem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tmmsystem.dto.dashboard.DirectorDashboardDTO;
import tmmsystem.dto.dashboard.PMDashboardDTO;
import tmmsystem.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get dashboard data for Director role
     * Includes: pending approvals, business overview, production metrics, charts
     */
    @GetMapping("/director")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ADMIN')")
    public ResponseEntity<DirectorDashboardDTO> getDirectorDashboard() {
        log.info("Director dashboard requested");
        DirectorDashboardDTO dashboard = dashboardService.getDirectorDashboard();
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get dashboard data for Production Manager role
     * Includes: alerts, stage progress, machine status, QC metrics, schedule
     */
    @GetMapping("/production-manager")
    @PreAuthorize("hasAnyRole('PRODUCTION_MANAGER', 'PRODUCTION', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<PMDashboardDTO> getPMDashboard() {
        log.info("Production Manager dashboard requested");
        PMDashboardDTO dashboard = dashboardService.getPMDashboard();
        return ResponseEntity.ok(dashboard);
    }
}
