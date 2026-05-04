import client from './client';

export interface StartUploadRequest {
  ownerId: string;
  courseId: string;
  sectionId: string;
  lessonId: string;
  courseName: string;
  sectionName: string;
  lessonTitle: string;
  fileName: string;
  mimeType?: string;
  totalSize: number;
  chunkSize: number;
}

export interface StartUploadResponse {
  sessionId: string;
  totalChunks: number;
  uploadedChunks: number;
  nextChunkIndex: number;
}

export interface UploadProgressResponse {
  sessionId: string;
  status: string;
  uploadedChunks: number;
  totalChunks: number;
  uploadedBytes: number;
  totalSize: number;
  percentComplete: number;
}

export interface CompleteUploadResponse {
  mediaId: string;
  url: string;
  localPath: string;
  fileName: string;
  fileType: string;
  mimeType: string;
  size: number;
}

export interface ResumeUploadResponse {
  sessionId: string;
  status: string;
  uploadedChunks: number;
  totalChunks: number;
  nextChunkIndex: number;
  missingChunks: number[];
}

export const CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB

export const startUpload = (data: StartUploadRequest) =>
  client.post<{ data: StartUploadResponse }>('/content/upload/start', data);

export const uploadChunk = (sessionId: string, chunkIndex: number, chunk: Blob) => {
  const form = new FormData();
  form.append('chunkIndex', chunkIndex.toString());
  form.append('data', chunk);
  return client.post<{ data: UploadProgressResponse }>(
    `/content/upload/${sessionId}/chunk`,
    form,
    { headers: { 'Content-Type': undefined } },
  );
};

export const completeUpload = (sessionId: string) =>
  client.post<{ data: CompleteUploadResponse }>(`/content/upload/${sessionId}/complete`);

export const pauseUpload = (sessionId: string) =>
  client.put<{ data: UploadProgressResponse }>(`/content/upload/${sessionId}/pause`);

export const resumeUpload = (sessionId: string) =>
  client.put<{ data: ResumeUploadResponse }>(`/content/upload/${sessionId}/resume`);

export const cancelUpload = (sessionId: string) =>
  client.delete(`/content/upload/${sessionId}`);

export const getUploadProgress = (sessionId: string) =>
  client.get<{ data: UploadProgressResponse }>(`/content/upload/${sessionId}/progress`);
