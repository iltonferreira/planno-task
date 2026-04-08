package com.planno.dash_api.service;

import com.planno.dash_api.entity.Project;
import com.planno.dash_api.entity.Subscription;
import com.planno.dash_api.entity.Task;
import com.planno.dash_api.entity.User;
import com.planno.dash_api.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;

    public void notifyTaskAssigned(Task task) {
        User responsible = task.getResponsibleUser();
        if (responsible == null || responsible.getEmail() == null || responsible.getEmail().isBlank()) {
            return;
        }

        emailService.sendAsync(
                responsible.getEmail(),
                "Nova tarefa atribuida: " + task.getTitle(),
                "task-assigned",
                Map.of(
                        "recipientName", responsible.getName(),
                        "taskTitle", task.getTitle(),
                        "projectName", task.getProject() == null ? "Sem projeto" : task.getProject().getName(),
                        "assignedBy", task.getCreatedBy().getName(),
                        "taskStatus", task.getStatus().name(),
                        "dueDate", task.getDueDate() == null ? "-" : task.getDueDate().toString()
                )
        );
    }

    public void notifyTaskStatusChanged(Task task, TaskStatus previousStatus) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>();
        if (task.getResponsibleUser() != null && task.getResponsibleUser().getEmail() != null) {
            recipients.add(task.getResponsibleUser().getEmail());
        }
        if (task.getCreatedBy() != null && task.getCreatedBy().getEmail() != null) {
            recipients.add(task.getCreatedBy().getEmail());
        }

        for (String recipient : recipients) {
            emailService.sendAsync(
                    recipient,
                    "Status atualizado: " + task.getTitle(),
                    "task-status-changed",
                    Map.of(
                            "taskTitle", task.getTitle(),
                            "projectName", task.getProject() == null ? "Sem projeto" : task.getProject().getName(),
                            "previousStatus", previousStatus.name(),
                            "newStatus", task.getStatus().name(),
                            "responsibleName", task.getResponsibleUser() == null ? "-" : task.getResponsibleUser().getName()
                    )
            );
        }
    }

    public void notifyProjectCreated(Project project) {
        User owner = project.getOwnerUser();
        if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
            return;
        }

        emailService.sendAsync(
                owner.getEmail(),
                "Novo projeto criado: " + project.getName(),
                "project-created",
                Map.of(
                        "recipientName", owner.getName(),
                        "projectName", project.getName(),
                        "clientName", project.getClient() == null ? "Sem cliente" : project.getClient().getName(),
                        "createdBy", project.getCreatedBy().getName(),
                        "projectStatus", project.getStatus().name()
                )
        );
    }

    public void notifySubscriptionUpdated(Subscription subscription) {
        if (subscription.getClient().getEmail() == null || subscription.getClient().getEmail().isBlank()) {
            return;
        }

        emailService.sendAsync(
                subscription.getClient().getEmail(),
                "Assinatura atualizada: " + subscription.getDescription(),
                "subscription-updated",
                Map.of(
                        "recipientName", subscription.getClient().getName(),
                        "subscriptionName", subscription.getDescription(),
                        "subscriptionStatus", subscription.getStatus().name(),
                        "nextBillingDate", subscription.getNextBillingDate() == null ? "-" : subscription.getNextBillingDate().toString(),
                        "price", String.valueOf(subscription.getPrice())
                )
        );
    }

    public void notifyPaymentReceived(String recipientEmail, String recipientName, String title, String amount, String paymentStatus) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }

        emailService.sendAsync(
                recipientEmail,
                "Pagamento recebido: " + title,
                "payment-received",
                Map.of(
                        "recipientName", recipientName,
                        "paymentTitle", title,
                        "paymentAmount", amount,
                        "paymentStatus", paymentStatus
                )
        );
    }
}
