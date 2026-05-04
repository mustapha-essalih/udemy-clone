import client from './client';
import type { CategoryOption } from '../pages/create-course/types';

interface ApiCategory {
  id: string;
  name: string;
  subCategories: { id: string; name: string }[];
}

export const fetchCategories = async (): Promise<CategoryOption[]> => {
  const res = await client.get<{ data: ApiCategory[] }>('/courses/categories');
  return res.data.data.map((c) => ({
    id: c.id,
    name: c.name,
    subCategories: c.subCategories,
  }));
};
