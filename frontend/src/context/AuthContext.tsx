import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import * as authApi from '../api/auth';

interface User {
  userId: string;
  email: string;
  role: string;
}

interface AuthContextValue {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string, role: string) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isManager: boolean;
  isInstructor: boolean;
  isStudent: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadUser(): User | null {
  const userId = localStorage.getItem('userId');
  const email = localStorage.getItem('email');
  const role = localStorage.getItem('role');
  if (userId && email && role) return { userId, email, role };
  return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(loadUser);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login({ email, password });
    const d = res.data.data;
    localStorage.setItem('accessToken', d.accessToken);
    localStorage.setItem('refreshToken', d.refreshToken);
    localStorage.setItem('userId', d.userId);
    localStorage.setItem('email', d.email);
    localStorage.setItem('role', d.role);
    setUser({ userId: d.userId, email: d.email, role: d.role });
  }, []);

  const register = useCallback(
    async (username: string, email: string, password: string, role: string) => {
      await authApi.register({ username, email, password, role });
    },
    []
  );

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken);
      } catch {

      }
    }
    localStorage.clear();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        register,
        logout,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'ADMIN',
        isManager: user?.role === 'MANAGER',
        isInstructor: user?.role === 'INSTRUCTOR',
        isStudent: user?.role === 'STUDENT',
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
