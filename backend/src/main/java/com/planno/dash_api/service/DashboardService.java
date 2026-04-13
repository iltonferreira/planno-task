package com.planno.dash_api.service;

import com.planno.dash_api.dto.response.DashboardSummaryResponseDTO;
import com.planno.dash_api.entity.Client;
import com.planno.dash_api.entity.Payment;
import com.planno.dash_api.entity.Subscription;
import com.planno.dash_api.entity.Task;
import com.planno.dash_api.enums.PaymentDirection;
import com.planno.dash_api.enums.PaymentStatus;
import com.planno.dash_api.repository.ClientRepository;
import com.planno.dash_api.repository.KnowledgeBasePageRepository;
import com.planno.dash_api.repository.PaymentRepository;
import com.planno.dash_api.repository.ProjectRepository;
import com.planno.dash_api.repository.SubscriptionRepository;
import com.planno.dash_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");

    private final CurrentUserService currentUserService;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final KnowledgeBasePageRepository knowledgeBasePageRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponseDTO getSummary() {
        Long tenantId = currentUserService.getCurrentTenantId();

        List<Client> clients = clientRepository.findAllByTenantIdAndActiveTrue(tenantId);
        List<Subscription> subscriptions = subscriptionRepository.findAllByTenantId(tenantId);
        List<Payment> payments = paymentRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        List<Task> tasks = taskRepository.findAllByTenantId(tenantId);

        BigDecimal recurringRevenue = subscriptions.stream()
                .filter(subscription -> "ACTIVE".equals(subscription.getStatus().name()))
                .map(Subscription::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overdueSubscriptions = subscriptions.stream()
                .filter(subscription -> "OVERDUE".equals(subscription.getStatus().name()))
                .map(Subscription::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal approvedIncome = sumPayments(payments, PaymentDirection.INCOME, PaymentStatus.APPROVED);
        BigDecimal approvedExpenses = sumPayments(payments, PaymentDirection.EXPENSE, PaymentStatus.APPROVED);
        BigDecimal pendingIncome = sumPayments(payments, PaymentDirection.INCOME, PaymentStatus.PENDING, PaymentStatus.IN_PROCESS);
        BigDecimal pendingExpenses = sumPayments(payments, PaymentDirection.EXPENSE, PaymentStatus.PENDING, PaymentStatus.IN_PROCESS);

        return new DashboardSummaryResponseDTO(
                clients.size(),
                projectRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                        .filter(project -> !"COMPLETED".equals(project.getStatus().name()) && !"CANCELLED".equals(project.getStatus().name()))
                        .count(),
                tasks.stream()
                        .filter(task -> !"DONE".equals(task.getStatus().name()) && !"CANCELLED".equals(task.getStatus().name()))
                        .count(),
                knowledgeBasePageRepository.findAllByTenantIdOrderByPinnedDescUpdatedAtDesc(tenantId).size(),
                recurringRevenue.doubleValue(),
                approvedIncome.doubleValue(),
                approvedExpenses.doubleValue(),
                pendingIncome.doubleValue(),
                pendingExpenses.doubleValue(),
                overdueSubscriptions.doubleValue(),
                approvedIncome.subtract(approvedExpenses).doubleValue(),
                buildRevenueByMonth(payments),
                buildPaymentsByStatus(payments),
                buildTopClients(clients),
                buildTasksByStatus(tasks)
        );
    }

    private BigDecimal sumPayments(List<Payment> payments, PaymentDirection direction, PaymentStatus... statuses) {
        List<PaymentStatus> acceptedStatuses = List.of(statuses);
        return payments.stream()
                .filter(payment -> payment.getDirection() == direction)
                .filter(payment -> acceptedStatuses.contains(payment.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DashboardSummaryResponseDTO.MetricValue> buildRevenueByMonth(List<Payment> payments) {
        Map<YearMonth, BigDecimal> totals = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();
        for (int index = 5; index >= 0; index--) {
            totals.put(currentMonth.minusMonths(index), BigDecimal.ZERO);
        }

        for (Payment payment : payments) {
            if (payment.getDirection() != PaymentDirection.INCOME || payment.getStatus() != PaymentStatus.APPROVED || payment.getPaidAt() == null) {
                continue;
            }

            YearMonth month = YearMonth.from(payment.getPaidAt());
            if (totals.containsKey(month)) {
                totals.put(month, totals.get(month).add(payment.getAmount()));
            }
        }

        return totals.entrySet().stream()
                .map(entry -> new DashboardSummaryResponseDTO.MetricValue(entry.getKey().format(MONTH_FORMATTER), entry.getValue().doubleValue()))
                .toList();
    }

    private List<DashboardSummaryResponseDTO.MetricValue> buildPaymentsByStatus(List<Payment> payments) {
        return payments.stream()
                .collect(Collectors.groupingBy(payment -> payment.getStatus().name(), LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DashboardSummaryResponseDTO.MetricValue(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<DashboardSummaryResponseDTO.ClientValue> buildTopClients(List<Client> clients) {
        return clients.stream()
                .map(client -> new DashboardSummaryResponseDTO.ClientValue(
                        client.getId(),
                        client.getName(),
                        calculateClientValue(client)
                ))
                .filter(client -> client.value() > 0)
                .sorted(Comparator.comparingDouble(DashboardSummaryResponseDTO.ClientValue::value).reversed())
                .limit(5)
                .toList();
    }

    private double calculateClientValue(Client client) {
        BigDecimal subscriptions = client.getSubscriptions() == null ? BigDecimal.ZERO : client.getSubscriptions().stream()
                .filter(subscription -> "ACTIVE".equals(subscription.getStatus().name()))
                .map(Subscription::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal payments = client.getPayments() == null ? BigDecimal.ZERO : client.getPayments().stream()
                .filter(payment -> payment.getDirection() == PaymentDirection.INCOME)
                .filter(payment -> payment.getStatus() == PaymentStatus.APPROVED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return subscriptions.add(payments).doubleValue();
    }

    private List<DashboardSummaryResponseDTO.MetricValue> buildTasksByStatus(List<Task> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(task -> task.getStatus().name(), LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DashboardSummaryResponseDTO.MetricValue(entry.getKey(), entry.getValue()))
                .toList();
    }
}
