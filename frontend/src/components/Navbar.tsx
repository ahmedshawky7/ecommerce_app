import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ShoppingCart,
  LogOut,
  ShieldCheck,
  Store,
  User as UserIcon,
  Menu,
  X,
} from 'lucide-react';
import { api } from '../lib/axios';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import { useI18n } from '../context/I18nContext';
import { ThemeAndLangControls } from './ui/ThemeAndLangControls';
import { formatNumber } from '../lib/utils';
import { CartResponse } from '../types/api';

export function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { connected } = useWebSocket();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const { data: cart } = useQuery({
    queryKey: ['cart'],
    queryFn: async () => (await api.get<CartResponse>('/api/cart')).data,
    enabled: isAuthenticated && !isAdmin,
  });

  const handleLogout = async () => {
    setMobileMenuOpen(false);
    await logout();
    navigate('/login');
  };

  const navLinkCls = ({ isActive }: { isActive: boolean }) =>
    `rounded-lg px-2.5 py-1.5 text-sm font-semibold transition-colors ${
      isActive ? 'bg-accent-soft text-accent dark:bg-accent/15' : 'text-ink-secondary hover:text-ink'
    }`;

  return (
    <header className="sticky top-0 z-40 w-full border-b border-line bg-surface/95 backdrop-blur-md">
      <div className="flex h-16 w-full items-center justify-between gap-3 px-4 md:px-6">
        {/* Brand + Desktop Links */}
        <div className="flex items-center gap-6">
          <Link
            to="/"
            onClick={() => setMobileMenuOpen(false)}
            className="flex shrink-0 items-center gap-2.5 text-lg font-extrabold tracking-tight text-ink sm:text-xl"
          >
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent text-accent-fg shadow-sm">
              <Store className="h-5 w-5" />
            </span>
            <span>{t('common.brand')}</span>
          </Link>

          <nav className="hidden items-center gap-1 md:flex" aria-label="Main navigation">
            <NavLink to="/" className={navLinkCls}>
              {t('nav.shop')}
            </NavLink>
            {isAuthenticated && !isAdmin && (
              <>
                <NavLink to="/orders" className={navLinkCls}>
                  {t('nav.myOrders')}
                </NavLink>
                <NavLink to="/wishlist" className={navLinkCls}>
                  {t('nav.wishlist')}
                </NavLink>
                <NavLink to="/addresses" className={navLinkCls}>
                  {t('nav.addresses')}
                </NavLink>
              </>
            )}
            {isAdmin && (
              <NavLink
                to="/admin"
                className="ms-2 inline-flex items-center gap-1.5 rounded-xl border border-accent/25 bg-accent-soft px-3 py-1.5 text-sm font-bold text-accent transition hover:bg-accent hover:text-accent-fg dark:bg-accent/15"
              >
                <ShieldCheck className="h-4 w-4" />
                <span>{t('nav.adminPanel')}</span>
              </NavLink>
            )}
          </nav>
        </div>

        {/* Desktop Actions */}
        <div className="hidden items-center gap-2.5 md:flex">
          {isAuthenticated && (
            <span
              title={connected ? t('nav.liveConnected') : t('nav.connecting')}
              className={`h-2.5 w-2.5 rounded-full ${
                connected ? 'bg-success' : 'bg-line-strong'
              }`}
            />
          )}

          <ThemeAndLangControls />

          {isAuthenticated && !isAdmin && (
            <Link
              to="/cart"
              className="relative inline-flex items-center gap-2 rounded-xl border border-line bg-surface px-3 py-2 text-sm font-semibold text-ink transition-colors hover:border-line-strong hover:bg-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
            >
              <ShoppingCart className="h-4 w-4 text-accent" />
              <span>{t('nav.cart')}</span>
              {(cart?.totalItems ?? 0) > 0 && (
                <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-accent px-1.5 text-xs font-bold text-accent-fg">
                  {formatNumber(cart?.totalItems)}
                </span>
              )}
            </Link>
          )}

          {isAuthenticated && user ? (
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-2 rounded-xl bg-subtle px-3 py-1.5 text-sm">
                <UserIcon className="h-4 w-4 text-ink-muted" />
                <span className="max-w-[120px] truncate font-semibold text-ink">
                  {user.username}
                </span>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                aria-label={t('nav.logout')}
                className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-surface px-3 py-2 text-sm font-semibold text-ink-secondary transition-colors hover:border-danger/30 hover:bg-danger-soft hover:text-danger focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
              >
                <LogOut className="h-4 w-4" />
                <span className="hidden lg:inline">{t('nav.logout')}</span>
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="rounded-xl px-3.5 py-2 text-sm font-semibold text-ink-secondary transition-colors hover:bg-subtle hover:text-ink"
              >
                {t('nav.signIn')}
              </Link>
              <Link
                to="/register"
                className="rounded-xl bg-accent px-4 py-2 text-sm font-bold text-accent-fg shadow-sm transition-colors hover:bg-accent-hover"
              >
                {t('nav.getStarted')}
              </Link>
            </div>
          )}
        </div>

        {/* Mobile Actions (fits cleanly on 320px+) */}
        <div className="flex items-center gap-2 md:hidden">
          {isAuthenticated && !isAdmin && (
            <Link
              to="/cart"
              aria-label={t('nav.cart')}
              className="relative flex items-center justify-center rounded-xl border border-line bg-surface p-2 text-ink"
            >
              <ShoppingCart className="h-5 w-5 text-accent" />
              {(cart?.totalItems ?? 0) > 0 && (
                <span className="absolute -end-1.5 -top-1.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-accent px-1 text-[10px] font-bold text-accent-fg">
                  {formatNumber(cart?.totalItems)}
                </span>
              )}
            </Link>
          )}

          <button
            type="button"
            onClick={() => setMobileMenuOpen((v) => !v)}
            aria-label={t('common.menu')}
            aria-expanded={mobileMenuOpen}
            className="rounded-xl border border-line bg-surface p-2 text-ink hover:bg-subtle"
          >
            {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="border-t border-line bg-surface px-4 py-4 md:hidden">
          <div className="flex flex-col space-y-2">
            <NavLink to="/" onClick={() => setMobileMenuOpen(false)} className={navLinkCls}>
              {t('nav.shop')}
            </NavLink>
            {isAuthenticated && !isAdmin && (
              <>
                <NavLink to="/orders" onClick={() => setMobileMenuOpen(false)} className={navLinkCls}>
                  {t('nav.myOrders')}
                </NavLink>
                <NavLink to="/wishlist" onClick={() => setMobileMenuOpen(false)} className={navLinkCls}>
                  {t('nav.wishlist')}
                </NavLink>
                <NavLink to="/addresses" onClick={() => setMobileMenuOpen(false)} className={navLinkCls}>
                  {t('nav.addresses')}
                </NavLink>
              </>
            )}
            {isAdmin && (
              <NavLink to="/admin" onClick={() => setMobileMenuOpen(false)} className={navLinkCls}>
                {t('nav.adminPanel')}
              </NavLink>
            )}

            <div className="border-t border-line pt-3">
              <ThemeAndLangControls />
            </div>

            <div className="border-t border-line pt-3">
              {isAuthenticated && user ? (
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 text-sm font-semibold text-ink">
                    <UserIcon className="h-4 w-4 text-ink-muted" />
                    <span className="truncate">{user.username}</span>
                  </div>
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="inline-flex items-center gap-1.5 rounded-xl border border-danger/25 bg-danger-soft/50 px-3 py-1.5 text-xs font-bold text-danger"
                  >
                    <LogOut className="h-3.5 w-3.5" />
                    <span>{t('nav.logout')}</span>
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <Link
                    to="/login"
                    onClick={() => setMobileMenuOpen(false)}
                    className="rounded-xl border border-line px-3 py-2 text-center text-sm font-semibold text-ink"
                  >
                    {t('nav.signIn')}
                  </Link>
                  <Link
                    to="/register"
                    onClick={() => setMobileMenuOpen(false)}
                    className="rounded-xl bg-accent px-3 py-2 text-center text-sm font-bold text-accent-fg"
                  >
                    {t('nav.getStarted')}
                  </Link>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}