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
  instructorId: string;
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

export const createCourse = (data: CreateCourseRequest) =>
  client.post<{ data: CourseResponse }>('/courses', data);

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
