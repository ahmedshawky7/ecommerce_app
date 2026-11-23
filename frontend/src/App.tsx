import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import { WebSocketProvider } from './context/WebSocketContext';
import { ThemeProvider, useTheme } from './context/ThemeContext';
import { I18nProvider } from './context/I18nContext';
import { CurrencyProvider } from './context/CurrencyContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { StoreLayout } from './components/StoreLayout';
import { AdminLayout } from './components/AdminLayout';

import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { ShopPage } from './pages/store/ShopPage';
import { ProductDetailPage } from './pages/store/ProductDetailPage';
import { CartPage } from './pages/store/CartPage';
import { CheckoutPage } from './pages/store/CheckoutPage';
import { CheckoutCancelPage, CheckoutSuccessPage } from './pages/store/CheckoutResultPages';
import { OrdersPage } from './pages/store/OrdersPage';
import { AddressesPage } from './pages/store/AddressesPage';
import { WishlistPage } from './pages/store/WishlistPage';

import { AdminOverviewPage } from './pages/admin/AdminOverviewPage';
import { AdminProductsPage } from './pages/admin/AdminProductsPage';
import { AdminOrdersPage } from './pages/admin/AdminOrdersPage';
import { AdminCouponsPage } from './pages/admin/AdminCouponsPage';
import { AdminUsersPage } from './pages/admin/AdminUsersPage';
import { AdminAuditLogsPage } from './pages/admin/AdminAuditLogsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

function ThemedToaster() {
  const { isDark } = useTheme();
  return (
    <Toaster
      position="top-right"
      toastOptions={{
        style: isDark
          ? {
              background: '#181C26',
              color: '#F3F4F6',
              border: '1px solid #2B3242',
            }
          : {
              background: '#FFFFFF',
              color: '#111827',
              border: '1px solid #E2E5EB',
            },
      }}
    />
  );
}

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <I18nProvider>
          <CurrencyProvider>
            <BrowserRouter>
              <AuthProvider>
                <WebSocketProvider>
                  <ThemedToaster />
                  <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />

                    <Route element={<StoreLayout />}>
                      <Route path="/" element={<ShopPage />} />
                      <Route path="/products/:id" element={<ProductDetailPage />} />

                      <Route element={<ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']} />}>
                        <Route path="/cart" element={<CartPage />} />
                        <Route path="/checkout" element={<CheckoutPage />} />
                        <Route path="/checkout/success" element={<CheckoutSuccessPage />} />
                        <Route path="/checkout/cancel" element={<CheckoutCancelPage />} />
                        <Route path="/orders" element={<OrdersPage />} />
                        <Route path="/addresses" element={<AddressesPage />} />
                        <Route path="/wishlist" element={<WishlistPage />} />
                      </Route>
                    </Route>

                    <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
                      <Route path="/admin" element={<AdminLayout />}>
                        <Route index element={<AdminOverviewPage />} />
                        <Route path="products" element={<AdminProductsPage />} />
                        <Route path="orders" element={<AdminOrdersPage />} />
                        <Route path="coupons" element={<AdminCouponsPage />} />
                        <Route path="users" element={<AdminUsersPage />} />
                        <Route path="audit-logs" element={<AdminAuditLogsPage />} />
                      </Route>
                    </Route>

                    <Route path="*" element={<Navigate to="/" replace />} />
                  </Routes>
                </WebSocketProvider>
              </AuthProvider>
            </BrowserRouter>
          </CurrencyProvider>
        </I18nProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export default App;