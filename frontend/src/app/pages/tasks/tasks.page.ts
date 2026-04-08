import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthStore } from '../../core/stores/auth.store';
import { ProjectsStore } from '../../core/stores/projects.store';
import { TasksStore } from '../../core/stores/tasks.store';
import { UsersStore } from '../../core/stores/users.store';
import { Task } from '../../core/models/domain.models';

@Component({
  selector: 'app-tasks-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './tasks.page.html',
  styleUrl: './tasks.page.scss'
})
export class TasksPageComponent {
  readonly authStore = inject(AuthStore);
  readonly tasksStore = inject(TasksStore);
  readonly projectsStore = inject(ProjectsStore);
  readonly usersStore = inject(UsersStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly statuses = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];
  readonly priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
  readonly showMineOnly = signal(false);
  readonly draggedTask = signal<Task | null>(null);
  readonly selectedParticipantIds = signal<number[]>([]);

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
    status: ['BACKLOG', Validators.required],
    priority: ['MEDIUM', Validators.required],
    dueDate: [''],
    projectId: [0],
    responsibleUserId: [0]
  });

  readonly filteredTasks = computed(() => {
    if (!this.showMineOnly()) {
      return this.tasksStore.items();
    }

    const currentUserId = this.authStore.user()?.id;
    return this.tasksStore.items().filter((task) => {
      if (!currentUserId) {
        return false;
      }

      return (
        task.responsibleUser?.id === currentUserId ||
        task.participants.some((participant) => participant.id === currentUserId) ||
        task.createdBy?.id === currentUserId
      );
    });
  });

  readonly columns = computed(() =>
    this.statuses.map((status) => ({
      status,
      tasks: this.filteredTasks()
        .filter((task) => task.status === status)
        .sort((left, right) => left.positionIndex - right.positionIndex)
    }))
  );

  toggleMineOnly(): void {
    this.showMineOnly.update((value) => !value);
  }

  toggleParticipant(userId: number, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;

    this.selectedParticipantIds.update((currentIds) => {
      if (checked) {
        return [...new Set([...currentIds, userId])];
      }

      return currentIds.filter((id) => id !== userId);
    });
  }

  isParticipantSelected(userId: number): boolean {
    return this.selectedParticipantIds().includes(userId);
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    await this.tasksStore.create({
      title: rawValue.title,
      description: rawValue.description,
      status: rawValue.status,
      priority: rawValue.priority,
      dueDate: rawValue.dueDate || null,
      positionIndex: this.columns().find((column) => column.status === rawValue.status)?.tasks.length ?? 0,
      projectId: rawValue.projectId || null,
      responsibleUserId: rawValue.responsibleUserId || null,
      participantIds: this.selectedParticipantIds()
    });

    this.form.reset({
      title: '',
      description: '',
      status: 'BACKLOG',
      priority: 'MEDIUM',
      dueDate: '',
      projectId: 0,
      responsibleUserId: 0
    });
    this.selectedParticipantIds.set([]);
  }

  startDragging(task: Task): void {
    this.draggedTask.set(task);
  }

  stopDragging(): void {
    this.draggedTask.set(null);
  }

  async dropOn(status: string): Promise<void> {
    const task = this.draggedTask();
    if (!task) {
      return;
    }

    const tasksInTarget = this.columns()
      .find((column) => column.status === status)
      ?.tasks.filter((item) => item.id !== task.id) ?? [];

    await this.tasksStore.updateStatus(task.id, status, tasksInTarget.length);
    this.stopDragging();
  }
}
