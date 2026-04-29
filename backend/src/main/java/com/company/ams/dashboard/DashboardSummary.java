package com.company.ams.dashboard;

import java.util.List;

public record DashboardSummary(
        Metrics metrics,
        List<AlertCard> alerts,
        List<RecentRequestCard> recentRequests,
        List<QuickActionCard> quickActions) {
    public record Metrics(
            long userTotal,
            long departmentTotal,
            long deviceAccountTotal,
            long pendingRequestTotal) {}

    public record AlertCard(
            String alertKey,
            String title,
            String description,
            long count) {}

    public record RecentRequestCard(
            long id,
            String requestNo,
            String requestType,
            String currentStatus,
            String createdAt) {}

    public record QuickActionCard(
            String actionKey,
            String title,
            String description) {}
}

