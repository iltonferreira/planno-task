import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthStore } from '../../core/stores/auth.store';
import { ClientsStore } from '../../core/stores/clients.store';
import { ProjectsStore } from '../../core/stores/projects.store';
import { UsersStore } from '../../core/stores/users.store';

@Component({
  selector: 'app-projects-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './projects.page.html',
  styleUrl: './projects.page.scss'
})
export class ProjectsPageComponent {
  readonly authStore = inject(AuthStore);
  readonly clientsStore = inject(ClientsStore);
  readonly projectsStore = inject(ProjectsStore);
  readonly usersStore = inject(UsersStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly statuses = ['PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    status: ['PLANNING', Validators.required],
    budget: [0],
    startDate: [''],
    endDate: [''],
    clientId: [0],
    ownerUserId: [0]
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    await this.projectsStore.create({
      name: rawValue.name,
      description: rawValue.description,
      status: rawValue.status,
      budget: rawValue.budget > 0 ? rawValue.budget : null,
      startDate: rawValue.startDate || null,
      endDate: rawValue.endDate || null,
      clientId: rawValue.clientId || null,
      ownerUserId: rawValue.ownerUserId || null
    });

    this.form.reset({
      name: '',
      description: '',
      status: 'PLANNING',
      budget: 0,
      startDate: '',
      endDate: '',
      clientId: 0,
      ownerUserId: 0
    });
  }
}
