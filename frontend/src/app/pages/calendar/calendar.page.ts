import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { GoogleCalendarEvent, Task } from '../../core/models/domain.models';
import { AuthStore } from '../../core/stores/auth.store';
import { GoogleCalendarStore } from '../../core/stores/google-calendar.store';
import { TasksStore } from '../../core/stores/tasks.store';

interface CalendarCell {
  isoDate: string;
  date: Date;
  dayNumber: number;
  inCurrentMonth: boolean;
  isToday: boolean;
  tasks: Task[];
  googleEvents: GoogleCalendarEvent[];
}

@Component({
  selector: 'app-calendar-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './calendar.page.html',
  styleUrl: './calendar.page.scss'
})
export class CalendarPageComponent {
  readonly authStore = inject(AuthStore);
  readonly tasksStore = inject(TasksStore);
  readonly googleCalendarStore = inject(GoogleCalendarStore);
  readonly weekDays = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sab'];
  readonly visibleMonth = signal(this.startOfMonth(new Date()));

  readonly myTasks = computed(() => this.tasksStore.mineItems());
  readonly googleEvents = computed(() => this.googleCalendarStore.events());
  readonly linkedTaskIds = computed(() => this.googleCalendarStore.linkedTaskIds());
  readonly scheduledTasks = computed(() =>
    this.myTasks()
      .filter((task) => Boolean(this.getTaskDateKey(task)))
      .sort((left, right) => this.compareDateKeys(this.getTaskDateKey(left), this.getTaskDateKey(right)))
  );
  readonly unscheduledTasks = computed(() =>
    this.myTasks().filter((task) => !this.getTaskDateKey(task))
  );
  readonly tasksByDate = computed(() => {
    const buckets = new Map<string, Task[]>();

    for (const task of this.scheduledTasks()) {
      const key = this.getTaskDateKey(task);
      if (!key) {
        continue;
      }

      const bucket = buckets.get(key) ?? [];
      bucket.push(task);
      bucket.sort((left, right) => {
        const priorityOrder = this.priorityWeight(right.priority) - this.priorityWeight(left.priority);
        if (priorityOrder !== 0) {
          return priorityOrder;
        }
        return left.title.localeCompare(right.title);
      });
      buckets.set(key, bucket);
    }

    return buckets;
  });
  readonly googleEventsByDate = computed(() => {
    const buckets = new Map<string, GoogleCalendarEvent[]>();

    for (const event of this.googleEvents()) {
      const key = this.getGoogleEventDateKey(event);
      if (!key) {
        continue;
      }

      const bucket = buckets.get(key) ?? [];
      bucket.push(event);
      bucket.sort((left, right) => this.compareDateKeys(this.getGoogleEventDateKey(left), this.getGoogleEventDateKey(right)));
      buckets.set(key, bucket);
    }

    return buckets;
  });
  readonly monthLabel = computed(() =>
    this.visibleMonth().toLocaleDateString('pt-BR', {
      month: 'long',
      year: 'numeric'
    })
  );
  readonly monthSummary = computed(() => {
    const monthKey = this.formatMonthKey(this.visibleMonth());
    const dueInMonth = this.scheduledTasks().filter((task) => this.getTaskDateKey(task)?.startsWith(monthKey));
    const googleInMonth = this.googleEvents().filter((event) => this.getGoogleEventDateKey(event)?.startsWith(monthKey));

    return {
      totalTasks: this.myTasks().length,
      scheduledTasks: this.scheduledTasks().length,
      googleEvents: googleInMonth.length,
      syncedTasks: this.linkedTaskIds().size,
      dueInMonth: dueInMonth.length
    };
  });
  readonly upcomingTasks = computed(() => {
    const todayKey = this.toIsoDate(new Date());
    return this.scheduledTasks()
      .filter((task) => (this.getTaskDateKey(task) ?? '') >= todayKey)
      .slice(0, 6);
  });
  readonly upcomingGoogleEvents = computed(() => {
    const todayKey = this.toIsoDate(new Date());
    return this.googleEvents()
      .filter((event) => (this.getGoogleEventDateKey(event) ?? '') >= todayKey)
      .slice(0, 6);
  });
  readonly externalOnlyEvents = computed(() =>
    this.googleEvents().filter((event) => event.linkedTaskId === null)
  );
  readonly mobileAgendaDays = computed(() =>
    this.calendarCells().filter(
      (cell) => cell.inCurrentMonth && (cell.isToday || cell.tasks.length > 0 || cell.googleEvents.length > 0)
    )
  );
  readonly calendarCells = computed(() => {
    const month = this.visibleMonth();
    const firstVisibleDay = new Date(month);
    firstVisibleDay.setDate(1 - month.getDay());

    return Array.from({ length: 42 }, (_, index) => {
      const current = new Date(firstVisibleDay);
      current.setDate(firstVisibleDay.getDate() + index);
      const isoDate = this.toIsoDate(current);

      return {
        isoDate,
        date: current,
        dayNumber: current.getDate(),
        inCurrentMonth: current.getMonth() === month.getMonth(),
        isToday: isoDate === this.toIsoDate(new Date()),
        tasks: this.tasksByDate().get(isoDate) ?? [],
        googleEvents: this.googleEventsByDate().get(isoDate) ?? []
      } satisfies CalendarCell;
    });
  });

  constructor() {
    effect(() => {
      if (this.authStore.isAuthenticated()) {
        void this.tasksStore.loadMine();
        void this.googleCalendarStore.loadStatus();
      } else {
        this.googleCalendarStore.reset();
      }
    });

    effect(() => {
      if (!this.authStore.isAuthenticated() || !this.googleCalendarStore.status()?.connected) {
        this.googleCalendarStore.events.set([]);
        return;
      }

      const { start, end } = this.visibleRange();
      void this.googleCalendarStore.loadEvents(start, end);
    });
  }

  goToPreviousMonth(): void {
    const previous = new Date(this.visibleMonth());
    previous.setMonth(previous.getMonth() - 1);
    this.visibleMonth.set(this.startOfMonth(previous));
  }

  goToNextMonth(): void {
    const next = new Date(this.visibleMonth());
    next.setMonth(next.getMonth() + 1);
    this.visibleMonth.set(this.startOfMonth(next));
  }

  goToCurrentMonth(): void {
    this.visibleMonth.set(this.startOfMonth(new Date()));
  }

  async connectGoogleCalendar(): Promise<void> {
    const authorizationUrl = await this.googleCalendarStore.createConnectUrl();
    window.location.href = authorizationUrl;
  }

  async disconnectGoogleCalendar(): Promise<void> {
    await this.googleCalendarStore.disconnect();
    await this.googleCalendarStore.loadStatus();
  }

  async syncTask(taskId: number): Promise<void> {
    await this.googleCalendarStore.syncTask(taskId);
    await this.reloadCalendarData();
  }

  async importEvent(calendarId: string, eventId: string): Promise<void> {
    await this.googleCalendarStore.importEvent(calendarId, eventId);
    await this.reloadCalendarData();
  }

  formatTaskDate(task: Task): string {
    const dateKey = this.getTaskDateKey(task);
    if (!dateKey) {
      return 'Sem prazo';
    }

    if (!task.allDay && task.startAt) {
      return this.toSafeDateTime(task.startAt).toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit'
      });
    }

    return this.toSafeDate(dateKey).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'short'
    });
  }

  formatGoogleEventDate(event: GoogleCalendarEvent): string {
    const dateKey = this.getGoogleEventDateKey(event);
    if (!dateKey) {
      return 'Sem horario';
    }

    if (!event.allDay && event.startAt) {
      return this.toSafeDateTime(event.startAt).toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit'
      });
    }

    return this.toSafeDate(dateKey).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'short'
    });
  }

  trackCell(_: number, cell: CalendarCell): string {
    return cell.isoDate;
  }

  formatAgendaDay(date: Date): string {
    return date.toLocaleDateString('pt-BR', {
      weekday: 'short',
      day: '2-digit',
      month: 'short'
    });
  }

  private async reloadCalendarData(): Promise<void> {
    await this.tasksStore.loadMine();
    await this.googleCalendarStore.loadStatus();
    if (this.googleCalendarStore.status()?.connected) {
      const { start, end } = this.visibleRange();
      await this.googleCalendarStore.loadEvents(start, end);
    }
  }

  private visibleRange(): { start: string; end: string } {
    const month = this.visibleMonth();
    const start = new Date(month);
    const end = new Date(month.getFullYear(), month.getMonth() + 1, 0);

    return {
      start: this.toIsoDate(start),
      end: this.toIsoDate(end)
    };
  }

  private startOfMonth(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), 1);
  }

  private formatMonthKey(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  private getTaskDateKey(task: Task): string | null {
    if (task.startAt) {
      return task.startAt.slice(0, 10);
    }
    return task.dueDate;
  }

  private getGoogleEventDateKey(event: GoogleCalendarEvent): string | null {
    if (event.startAt) {
      return event.startAt.slice(0, 10);
    }
    return event.startDate;
  }

  private compareDateKeys(left: string | null | undefined, right: string | null | undefined): number {
    return (left ?? '').localeCompare(right ?? '');
  }

  private toIsoDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
      date.getDate()
    ).padStart(2, '0')}`;
  }

  private toSafeDate(value: string): Date {
    return new Date(`${value}T12:00:00`);
  }

  private toSafeDateTime(value: string): Date {
    return new Date(value);
  }

  private priorityWeight(priority: string): number {
    switch (priority) {
      case 'URGENT':
        return 4;
      case 'HIGH':
        return 3;
      case 'MEDIUM':
        return 2;
      default:
        return 1;
    }
  }
}
