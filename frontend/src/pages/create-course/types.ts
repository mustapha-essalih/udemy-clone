export interface BasicInfo {
  title: string;
  subtitle: string;
  description: string;
  category: string;
  subcategory: string;
  level: string;
  language: string;
  free: boolean;
  price: string;
}

export interface DraftLesson {
  draftId: string;
  title: string;
  type: 'video' | 'pdf' | 'article';
}

export interface DraftSection {
  draftId: string;
  title: string;
  lessons: DraftLesson[];
}

export interface UploadTarget {
  lessonId: string;
  sectionId: string;
  courseId: string;
  lessonTitle: string;
  sectionTitle: string;
  type: 'video' | 'pdf' | 'article';
}

export interface UploadState {
  file: File | null;
  sessionId: string | null;
  status: 'idle' | 'starting' | 'uploading' | 'paused' | 'done' | 'error';
  percent: number;
  speed: number;
  errorMsg: string;
  linkedUrl: string | null;
}

export interface ValidationErrors {
  title?: string;
  description?: string;
  category?: string;
  subcategory?: string;
  price?: string;
}

export interface CategoryOption {
  id: string;
  name: string;
  subCategories: { id: string; name: string }[];
}

export const LEVEL_MAP: Record<string, string> = {
  beginner:     'BEGINNER',
  intermediate: 'INTERMEDIATE',
  advanced:     'ADVANCED',
  all:          'ALL_LEVELS',
};

export const LESSON_TYPE_MAP: Record<DraftLesson['type'], string> = {
  video:   'VIDEO',
  pdf:     'TEXT',
  article: 'ARTICLE',
};

export const fmtBytes = (b: number): string => {
  if (!b) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0; let v = b;
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
  return `${v.toFixed(v >= 10 || i === 0 ? 0 : 1)} ${units[i]}`;
};

export const fmtSpeed = (bps: number): string => fmtBytes(bps) + '/s';

export const fmtETA = (secs: number): string => {
  if (!secs || !isFinite(secs)) return '—';
  if (secs < 60) return `${Math.round(secs)}s`;
  return `${Math.floor(secs / 60)}m ${Math.round(secs % 60)}s`;
};

export const fmtDuration = (minutes: number): string => {
  if (!minutes) return '—';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h ? `${h}h ${m}m` : `${m}m`;
};

export function validateBasicInfo(info: BasicInfo): ValidationErrors {
  const errors: ValidationErrors = {};
  if (!info.title || info.title.length < 5) errors.title = 'Title must be at least 5 characters.';
  if (!info.description || info.description.length < 50) errors.description = 'Description must be at least 50 characters.';
  if (!info.category) errors.category = 'Pick a category.';
  if (info.category && !info.subcategory) errors.subcategory = 'Pick a subcategory.';
  if (!info.free && !info.price) errors.price = 'Enter a price or make the course free.';
  return errors;
}
