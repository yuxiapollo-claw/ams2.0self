package com.company.ams.dashboard;

import com.company.ams.common.persistence.DashboardRepository;
import com.company.ams.common.persistence.DashboardRepository.AlertRow;
import com.company.ams.common.persistence.DashboardRepository.RecentRequestRow;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardSummary summary() {
        DashboardSummary.Metrics metrics = new DashboardSummary.Metrics(
                dashboardRepository.userTotal(),
                dashboardRepository.departmentTotal(),
                dashboardRepository.deviceAccountTotal(),
                dashboardRepository.pendingRequestTotal());

        List<DashboardSummary.AlertCard> alerts = dashboardRepository.findAlerts().stream()
                .map(this::toAlertCard)
                .toList();

        List<DashboardSummary.RecentRequestCard> recentRequests = dashboardRepository.findRecentRequests(5).stream()
                .map(this::toRecentRequestCard)
                .toList();

        List<DashboardSummary.QuickActionCard> quickActions = List.of(
                new DashboardSummary.QuickActionCard("create-request", "Create Request", "Start a new access request"),
                new DashboardSummary.QuickActionCard("create-user", "Create User", "Add a new user"));

        return new DashboardSummary(
                metrics,
                alerts,
                recentRequests,
                quickActions);
    }

    private DashboardSummary.AlertCard toAlertCard(AlertRow row) {
        return new DashboardSummary.AlertCard(
                row.alertKey(),
                row.title(),
                row.description(),
                row.count());
    }

    private DashboardSummary.RecentRequestCard toRecentRequestCard(RecentRequestRow row) {
        String createdAtIso = row.createdAt() == null ? null : row.createdAt().toString();
        return new DashboardSummary.RecentRequestCard(
                row.id(),
                row.requestNo(),
                row.requestType(),
                row.currentStatus(),
                createdAtIso);
    }
}
