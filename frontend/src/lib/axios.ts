import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { AuthResponse } from '../types/api';

export const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

const STORAGE_KEY = 'novacart_auth';

export function getStoredAuth(): AuthResponse | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  } catch {
    return null;
  }
}

export function setStoredAuth(auth: AuthResponse | null): void {
  if (auth) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
  window.dispatchEvent(new Event('auth-storage-updated'));
}

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = getStoredAuth();
  if (auth?.token && config.headers) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

interface RetryConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    }
  });
  failedQueue = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryConfig | undefined;
    if (!originalRequest || error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    const url = originalRequest.url || '';
    if (url.includes('/api/auth/login') || url.includes('/api/auth/refresh')) {
      return Promise.reject(error);
    }

    const currentAuth = getStoredAuth();
    if (!currentAuth?.refreshToken) {
      setStoredAuth(null);
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((newToken) => {
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
        }
        return api(originalRequest);
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const res = await axios.post<AuthResponse>(`${API_BASE_URL}/api/auth/refresh`, {
        refreshToken: currentAuth.refreshToken,
      });
      setStoredAuth(res.data);
      processQueue(null, res.data.token);
      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${res.data.token}`;
      }
      return api(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      setStoredAuth(null);
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);