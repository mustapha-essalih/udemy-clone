import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
  requireInstructor?: boolean;
}

export default function ProtectedRoute({ children, requireInstructor }: Props) {
  const { isAuthenticated, isInstructor } = useAuth();

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (requireInstructor && !isInstructor) return <Navigate to="/dashboard" replace />;

  return <>{children}</>;
}
