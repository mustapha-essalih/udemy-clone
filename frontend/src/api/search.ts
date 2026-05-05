import client from './client';

export type SortOption = 'RELEVANCE' | 'RATING' | 'NEWEST' | 'POPULARITY' | 'PRICE_ASC' | 'PRICE_DESC';
export type PriceFilter = 'FREE' | 'PAID' | 'ALL';
export type DurationBucket = 'SHORT' | 'MEDIUM' | 'LONG';

export interface CourseHit {
  courseId: string;
  title: string;
  description: string;
  instructorName: string;
  instructorId: string;
  category: string;
  subcategory: string;
  rating: number | null;
  ratingCount: number | null;
  enrollments: number | null;
  language: string;
  durationMinutes: number | null;
  price: number | null;
  isFree: boolean;
  level: string;
  createdAt: string;
  score: number | null;
}

export interface FacetBucket {
  key: string;
  count: number;
}

export interface SearchResponse {
  total: number;
  page: number;
  size: number;
  hits: CourseHit[];
  facets: Record<string, FacetBucket[]>;
  tookMillis: number;
}

export interface AutocompleteSuggestion {
  text: string;
  type: string;
  referenceId: string | null;
}

export interface AutocompleteResponse {
  courses: AutocompleteSuggestion[];
  instructors: AutocompleteSuggestion[];
  categories: AutocompleteSuggestion[];
  popularTerms: AutocompleteSuggestion[];
}

export interface SearchParams {
  q?: string;
  category?: string[];
  language?: string[];
  level?: string[];
  minRating?: number;
  price?: PriceFilter;
  duration?: DurationBucket[];
  sort?: SortOption;
  page?: number;
  size?: number;
}

export const searchCourses = (params: SearchParams) => {
  const p: Record<string, unknown> = {};
  if (params.q) p['q'] = params.q;
  if (params.category?.length) p['category'] = params.category;
  if (params.language?.length) p['language'] = params.language;
  if (params.level?.length) p['level'] = params.level;
  if (params.minRating != null) p['minRating'] = params.minRating;
  if (params.price && params.price !== 'ALL') p['price'] = params.price;
  if (params.duration?.length) p['duration'] = params.duration;
  if (params.sort) p['sort'] = params.sort;
  if (params.page != null) p['page'] = params.page;
  if (params.size != null) p['size'] = params.size;
  return client.get<{ data: SearchResponse }>('/search/courses', { params: p });
};

export const autocomplete = (prefix: string) =>
  client.get<{ data: AutocompleteResponse }>('/search/autocomplete', { params: { q: prefix } });
