import client from './client';

export interface LessonResponse {
  lessonId: string;
  sectionId: string;
  title: string;
  lessonType: string;
  videoUrl: string | null;
  textUrl: string | null;
  durationMinutes: number | null;
  isPreview: boolean | null;
  createdAt: string;
}

export interface SectionResponse {
  sectionId: string;
  courseId: string;
  title: string;
  lessons: LessonResponse[];
}

export interface CourseResponse {
  courseId: string;
  instructorId: string;
  title: string;
  subTitle: string;
  description: string;
  price: number;
  isFree: boolean;
  language: string;
  couponCode: string | null;
  rating: number;
  courseDurationMinutes: number;
  status: string;
  level: string;
  sections: SectionResponse[];
  createdAt: string;
}

export interface CreateLessonRequest {
  title: string;
  lessonType: 'VIDEO' | 'TEXT' | 'ARTICLE';
}

export interface CreateSectionRequest {
  title: string;
  lessons: CreateLessonRequest[];
}

export interface CreateCourseRequest {
  title: string;
  subTitle: string;
  description: string;
  price: number;
  isFree?: boolean;
  language: string;
  couponCode?: string;
  courseDurationMinutes: number;
  level?: string;
  sections: CreateSectionRequest[];
}

export interface DraftLessonResponse {
  lessonId: string;
  title: string;
  lessonType: string;
  videoUrl: string | null;
  durationMinutes: number | null;
  isPreview: boolean | null;
  textUrl: string | null;
}

export interface DraftSectionResponse {
  sectionId: string;
  title: string;
  lessons: DraftLessonResponse[];
}

export interface DraftResponse {
  draftId: string;
  instructorId: string;
  title: string;
  subTitle: string;
  description: string;
  price: number;
  isFree: boolean;
  language: string;
  couponCode: string | null;
  courseDurationMinutes: number;
  level: string;
  sections: DraftSectionResponse[];
  status: string;
  rejectionFeedback: string | null;
  createdAt: string;
  updatedAt: string;
  submittedAt: string | null;
  reviewedAt: string | null;
}

export interface LessonOverview {
  lessonId: string;
  title: string;
  lessonType: string;
  videoUrl: string | null;
  textUrl: string | null;
  durationMinutes: number | null;
  isPreview: boolean | null;
}

export interface SectionOverview {
  sectionId: string;
  title: string;
  lessons: LessonOverview[];
}

export interface CourseOverview {
  id: string;
  instructorId: string;
  title: string;
  subTitle: string | null;
  description: string | null;
  price: number | null;
  isFree: boolean | null;
  language: string | null;
  couponCode: string | null;
  rating: number | null;
  courseDurationMinutes: number | null;
  level: string | null;
  status: string;
  source: 'REDIS' | 'POSTGRES';
  rejectionFeedback: string | null;
  createdAt: string;
  updatedAt: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  sections: SectionOverview[];
}

export const listAllCourses = () =>
  client.get<{ data: CourseOverview[] }>('/courses/manager/all');

export const approveDraft = (draftId: string) =>
  client.post<{ data: unknown }>(`/courses/manager/${draftId}/approve`);

export const rejectDraft = (draftId: string, feedback: string) =>
  client.post<{ data: DraftResponse }>(`/courses/manager/${draftId}/reject`, { feedback });

export const createCourse = (data: CreateCourseRequest) =>
  client.post<{ data: DraftResponse }>('/courses', data);

export const getCourse = (id: string) =>
  client.get<{ data: CourseResponse }>(`/courses/${id}`);

export const listCourses = (instructorId: string) =>
  client.get<{ data: CourseResponse[] }>(`/courses?instructorId=${instructorId}`);

export const updateVideoUrl = (
  courseId: string,
  sectionId: string,
  lessonId: string,
  data: { videoUrl: string; durationMinutes?: number; isPreview?: boolean }
) =>
  client.put<{ data: LessonResponse }>(
    `/courses/${courseId}/sections/${sectionId}/lessons/${lessonId}/video-url`,
    data
  );

export const updateTextUrl = (
  courseId: string,
  sectionId: string,
  lessonId: string,
  data: { contentUrl: string }
) =>
  client.put<{ data: LessonResponse }>(
    `/courses/${courseId}/sections/${sectionId}/lessons/${lessonId}/text-url`,
    data
  );

export const updateDraftVideoUrl = (
  draftId: string,
  sectionId: string,
  lessonId: string,
  data: { videoUrl: string; durationMinutes?: number; isPreview?: boolean }
) =>
  client.put<{ data: DraftResponse }>(
    `/courses/drafts/${draftId}/sections/${sectionId}/lessons/${lessonId}/video-url`,
    data
  );

export const updateDraftTextUrl = (
  draftId: string,
  sectionId: string,
  lessonId: string,
  data: { contentUrl: string }
) =>
  client.put<{ data: DraftResponse }>(
    `/courses/drafts/${draftId}/sections/${sectionId}/lessons/${lessonId}/text-url`,
    data
  );
