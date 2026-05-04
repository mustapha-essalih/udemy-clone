import client from './client';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
  role: string;
}

export interface RegistrationResponse {
  userId: string;
  username: string;
  email: string;
  role: string;
}

export const register = (data: {
  username: string;
  email: string;
  password: string;
  role: string;
}) => client.post<{ data: RegistrationResponse }>('/auth/register', data);

export const login = (data: { email: string; password: string }) =>
  client.post<{ data: AuthResponse }>('/auth/login', data);

export const logout = (refreshToken: string) =>
  client.post('/auth/logout', { refreshToken });
