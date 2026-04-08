import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { KnowledgeBaseStore } from '../../core/stores/knowledge-base.store';

@Component({
  selector: 'app-knowledge-base-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './knowledge-base.page.html',
  styleUrl: './knowledge-base.page.scss'
})
export class KnowledgeBasePageComponent {
  private readonly formBuilder = inject(FormBuilder);
  readonly knowledgeBaseStore = inject(KnowledgeBaseStore);
  readonly selectedPageId = signal<number | null>(null);

  readonly noteTemplates = [
    'Checklist de onboarding do cliente',
    'Playbook de rotina semanal',
    'Resumo de sprint e aprendizados'
  ];
  readonly selectedPage = computed(() =>
    this.knowledgeBaseStore.items().find((item) => item.id === this.selectedPageId()) ?? null
  );

  readonly form = this.formBuilder.nonNullable.group({
    title: ['Playbook de entrega mensal', Validators.required],
    summary: [''],
    pinned: [false],
    body: [
      '## Visao geral\n\nMapeie entregas, dependencias e aprovacoes.\n\n## Blocos fixos\n\n- Atualizar indicadores\n- Revisar backlog\n- Consolidar financeiro\n',
      Validators.required
    ]
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    const payload = {
      title: rawValue.title,
      summary: rawValue.summary || null,
      content: rawValue.body,
      pinned: rawValue.pinned
    };

    if (this.selectedPageId()) {
      const updated = await this.knowledgeBaseStore.update(this.selectedPageId()!, payload);
      this.selectPage(updated.id);
      return;
    }

    const created = await this.knowledgeBaseStore.create(payload);
    this.selectPage(created.id);
  }

  selectPage(pageId: number): void {
    const page = this.knowledgeBaseStore.items().find((item) => item.id === pageId);
    if (!page) {
      return;
    }

    this.selectedPageId.set(pageId);
    this.form.reset({
      title: page.title,
      summary: page.summary ?? '',
      pinned: page.pinned,
      body: page.content
    });
  }

  createNew(): void {
    this.selectedPageId.set(null);
    this.form.reset({
      title: 'Playbook de entrega mensal',
      summary: '',
      pinned: false,
      body: '## Visao geral\n\nMapeie entregas, dependencias e aprovacoes.\n\n## Blocos fixos\n\n- Atualizar indicadores\n- Revisar backlog\n- Consolidar financeiro\n'
    });
  }

  async remove(pageId: number): Promise<void> {
    await this.knowledgeBaseStore.remove(pageId);
    if (this.selectedPageId() === pageId) {
      this.createNew();
    }
  }
}
