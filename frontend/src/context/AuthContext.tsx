import React, { createContext, useContext, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { api, getStoredAuth, setStoredAuth } from '../lib/axios';
import { AuthResponse } from '../types/api';

interface AuthContextValue {
  user: AuthResponse | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<AuthResponse>;
  register: (username: string, email: string, password: string) => Promise<AuthResponse>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(() => getStoredAuth());
  const queryClient = useQueryClient();

  useEffect(() => {
    const syncAuth = () => setUser(getStoredAuth());
    window.addEventListener('auth-storage-updated', syncAuth);
    window.addEventListener('storage', syncAuth);
    return () => {
      window.removeEventListener('auth-storage-updated', syncAuth);
      window.removeEventListener('storage', syncAuth);
    };
  }, []);

  const login = async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password });
    setStoredAuth(data);
    setUser(data);
    queryClient.invalidateQueries();
    return data;
  };

  const register = async (username: string, email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/api/auth/register', {
      username,
      email,
      password,
    });
    setStoredAuth(data);
    setUser(data);
    queryClient.invalidateQueries();
    return data;
  };

  const logout = async () => {
    const current = getStoredAuth();
    try {
      if (current?.refreshToken) {
        await api.post('/api/auth/logout', { refreshToken: current.refreshToken });
      }
    } catch {
      // Ignore logout network error when clearing local session
    } finally {
      setStoredAuth(null);
      setUser(null);
      queryClient.clear();
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: Boolean(user?.token),
        isAdmin: user?.role === 'ADMIN',
        login,
        register,
        logout,
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