package com.planno.dash_api.dto.response;

import java.util.List;

public record DashboardSummaryResponseDTO(
        long totalClients,
        long activeProjects,
        long openTasks,
        long knowledgeBaseEntries,
        double recurringRevenue,
        double approvedIncome,
        double approvedExpenses,
        double pendingIncome,
        double pendingExpenses,
        double overdueSubscriptions,
        double netCashflow,
        List<MetricValue> revenueByMonth,
        List<MetricValue> paymentsByStatus,
        List<ClientValue> topClients,
        List<MetricValue> tasksByStatus
) {
    public record MetricValue(
            String label,
            double value
    ) {
    }

    public record ClientValue(
            Long clientId,
            String clientName,
            double value
    ) {
    }
}
