import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { DocumentAsset, GoogleDriveConnectionStatus } from '../models/domain.models';
import { getApiErrorMessage } from '../utils/api-error';

export interface DocumentUploadPayload {
  relationType: string;
  relationId: number | null;
  file: File;
}

@Injectable({ providedIn: 'root' })
export class DocumentsStore {
  private readonly http = inject(HttpClient);

  readonly items = signal<DocumentAsset[]>([]);
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);
  readonly integrationStatus = signal<GoogleDriveConnectionStatus | null>(null);
  readonly integrationLoading = signal(false);
  readonly integrationBusy = signal(false);
  readonly integrationError = signal<string | null>(null);

  async load(relationType?: string | null, relationId?: number | null): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const params = new URLSearchParams();
      if (relationType) {
        params.set('relationType', relationType);
      }
      if (relationId) {
        params.set('relationId', String(relationId));
      }

      const query = params.toString();
      const suffix = query ? `?${query}` : '';
      const [documents, integrationStatus] = await Promise.all([
        firstValueFrom(this.http.get<DocumentAsset[]>(`/api/documents${suffix}`)),
        firstValueFrom(this.http.get<GoogleDriveConnectionStatus>('/api/integrations/google-drive/status'))
      ]);
      this.items.set(documents);
      this.integrationStatus.set(integrationStatus);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel carregar os documentos.'));
    } finally {
      this.loading.set(false);
    }
  }

  async loadIntegrationStatus(): Promise<void> {
    this.integrationLoading.set(true);
    this.integrationError.set(null);

    try {
      const status = await firstValueFrom(
        this.http.get<GoogleDriveConnectionStatus>('/api/integrations/google-drive/status')
      );
      this.integrationStatus.set(status);
    } catch (error) {
      this.integrationError.set(getApiErrorMessage(error, 'Nao foi possivel consultar a conexao do Google Drive.'));
    } finally {
      this.integrationLoading.set(false);
    }
  }

  async createConnectUrl(): Promise<string> {
    this.integrationBusy.set(true);
    this.integrationError.set(null);

    try {
      const response = await firstValueFrom(
        this.http.post<{ authorizationUrl: string }>('/api/integrations/google-drive/connect-url', {})
      );
      return response.authorizationUrl;
    } catch (error) {
      this.integrationError.set(getApiErrorMessage(error, 'Nao foi possivel iniciar a conexao com o Google Drive.'));
      throw error;
    } finally {
      this.integrationBusy.set(false);
    }
  }

  async disconnect(): Promise<void> {
    this.integrationBusy.set(true);
    this.integrationError.set(null);

    try {
      await firstValueFrom(this.http.delete<void>('/api/integrations/google-drive/connection'));
      await this.loadIntegrationStatus();
    } catch (error) {
      this.integrationError.set(getApiErrorMessage(error, 'Nao foi possivel desconectar o Google Drive.'));
      throw error;
    } finally {
      this.integrationBusy.set(false);
    }
  }

  async upload(payload: DocumentUploadPayload): Promise<void> {
    this.uploading.set(true);
    this.error.set(null);

    try {
      const formData = new FormData();
      formData.append('file', payload.file);

      const params = new URLSearchParams({ relationType: payload.relationType });
      if (payload.relationId) {
        params.set('relationId', String(payload.relationId));
      }

      const created = await firstValueFrom(
        this.http.post<DocumentAsset>(`/api/documents/upload?${params.toString()}`, formData)
      );

      this.items.update((items) => [created, ...items]);
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel enviar o documento.'));
      throw error;
    } finally {
      this.uploading.set(false);
    }
  }

  async download(documentId: number): Promise<Blob> {
    this.error.set(null);

    try {
      return await firstValueFrom(
        this.http.get(`/api/documents/${documentId}/download`, { responseType: 'blob' })
      );
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel baixar o documento.'));
      throw error;
    }
  }

  async remove(documentId: number): Promise<void> {
    this.error.set(null);

    try {
      await firstValueFrom(this.http.delete<void>(`/api/documents/${documentId}`));
      this.items.update((items) => items.filter((item) => item.id !== documentId));
    } catch (error) {
      this.error.set(getApiErrorMessage(error, 'Nao foi possivel remover o documento.'));
      throw error;
    }
  }

  reset(): void {
    this.items.set([]);
    this.loading.set(false);
    this.uploading.set(false);
    this.error.set(null);
    this.integrationStatus.set(null);
    this.integrationLoading.set(false);
    this.integrationBusy.set(false);
    this.integrationError.set(null);
  }
}
