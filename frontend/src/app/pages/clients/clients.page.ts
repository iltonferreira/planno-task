import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthStore } from '../../core/stores/auth.store';
import { ClientsStore } from '../../core/stores/clients.store';

@Component({
  selector: 'app-clients-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './clients.page.html',
  styleUrl: './clients.page.scss'
})
export class ClientsPageComponent {
  readonly authStore = inject(AuthStore);
  readonly clientsStore = inject(ClientsStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    document: ['']
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    await this.clientsStore.create(this.form.getRawValue());
    this.form.reset({
      name: '',
      email: '',
      phone: '',
      document: ''
    });
  }

  async remove(clientId: number): Promise<void> {
    await this.clientsStore.remove(clientId);
  }
}
