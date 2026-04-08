import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { DocumentsStore } from '../../core/stores/documents.store';
import { ClientsStore } from '../../core/stores/clients.store';
import { KnowledgeBaseStore } from '../../core/stores/knowledge-base.store';
import { ProjectsStore } from '../../core/stores/projects.store';

@Component({
  selector: 'app-documents-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './documents.page.html',
  styleUrl: './documents.page.scss'
})
export class DocumentsPageComponent {
  readonly documentsStore = inject(DocumentsStore);
  readonly clientsStore = inject(ClientsStore);
  readonly projectsStore = inject(ProjectsStore);
  readonly knowledgeBaseStore = inject(KnowledgeBaseStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly queuedFiles = signal<File[]>([]);
  readonly folders = [
    '/Clients/{name}',
    '/Projects/{name}',
    '/Finance',
    '/KnowledgeBase/{slug}',
    '/General'
  ];
  readonly relationType = signal('FINANCE');
  readonly driveStatus = computed(() => this.documentsStore.integrationStatus());
  readonly availableRelations = computed<Array<{ id: number; label: string }>>(() => {
    switch (this.relationType()) {
      case 'CLIENT':
        return this.clientsStore.items().map((item) => ({ id: item.id, label: item.name }));
      case 'PROJECT':
        return this.projectsStore.items().map((item) => ({ id: item.id, label: item.name }));
      case 'KNOWLEDGE_BASE':
        return this.knowledgeBaseStore.items().map((item) => ({ id: item.id, label: item.title }));
      default:
        return [];
    }
  });

  readonly form = this.formBuilder.nonNullable.group({
    relationType: ['FINANCE'],
    relationId: [0]
  });

  handleFileInput(event: Event): void {
    const files = Array.from((event.target as HTMLInputElement).files ?? []);
    this.appendFiles(files);
  }

  handleDrop(event: DragEvent): void {
    event.preventDefault();
    const files = Array.from(event.dataTransfer?.files ?? []);
    this.appendFiles(files);
  }

  preventDefault(event: DragEvent): void {
    event.preventDefault();
  }

  private appendFiles(files: File[]): void {
    if (!files.length) {
      return;
    }

    this.queuedFiles.update((currentFiles) => [...files, ...currentFiles]);
  }

  async uploadQueued(): Promise<void> {
    if (!this.documentsStore.integrationStatus()?.connected) {
      return;
    }

    const files = this.queuedFiles();
    if (!files.length) {
      return;
    }

    const rawValue = this.form.getRawValue();
    const relationType = rawValue.relationType;
    const relationId = ['CLIENT', 'PROJECT', 'KNOWLEDGE_BASE'].includes(relationType) ? rawValue.relationId || null : null;

    for (const file of files) {
      await this.documentsStore.upload({
        relationType,
        relationId,
        file
      });
    }

    this.queuedFiles.set([]);
  }

  async download(documentId: number, fileName: string): Promise<void> {
    const blob = await this.documentsStore.download(documentId);
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  async remove(documentId: number): Promise<void> {
    await this.documentsStore.remove(documentId);
  }

  async connectDrive(): Promise<void> {
    const url = await this.documentsStore.createConnectUrl();
    window.location.href = url;
  }

  async disconnectDrive(): Promise<void> {
    await this.documentsStore.disconnect();
  }

  updateRelationType(type: string): void {
    this.relationType.set(type);
    this.form.patchValue({ relationType: type, relationId: 0 });
  }

  formatQueuedFileSize(file: File): string {
    return `${Math.max(1, Math.round(file.size / 1024))} KB`;
  }
}
