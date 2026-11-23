import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Package,
  ShoppingBag,
  TicketPercent,
  Users,
  FileClock,
  Store,
  LogOut,
  Bell,
  Menu,
  X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import { useI18n } from '../context/I18nContext';
import { ThemeAndLangControls } from './ui/ThemeAndLangControls';
import { Badge } from './ui/Badge';
import { formatNumber } from '../lib/utils';

export function AdminLayout() {
  const { user, logout } = useAuth();
  const { connected, adminAlerts, clearAdminAlerts } = useWebSocket();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);

  const navItems = [
    { to: '/admin', label: t('adminNav.overview'), icon: LayoutDashboard, end: true },
    { to: '/admin/products', label: t('adminNav.products'), icon: Package },
    { to: '/admin/orders', label: t('adminNav.orders'), icon: ShoppingBag },
    { to: '/admin/coupons', label: t('adminNav.coupons'), icon: TicketPercent },
    { to: '/admin/users', label: t('adminNav.users'), icon: Users },
    { to: '/admin/audit-logs', label: t('adminNav.auditLogs'), icon: FileClock },
  ];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const SidebarContent = (
    <div className="flex h-full flex-col">
      <div className="flex h-16 items-center justify-between border-b border-line px-5">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent font-bold text-accent-fg shadow-sm">
            N
          </div>
          <div>
            <p className="text-sm font-bold leading-none text-ink">{t('nav.adminTitle')}</p>
            <p className="mt-1 text-xs text-ink-muted">{t('nav.adminSubtitle')}</p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => setDrawerOpen(false)}
          aria-label={t('common.close')}
          className="rounded-lg p-1.5 text-ink-muted hover:bg-subtle lg:hidden"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      <nav className="flex-1 space-y-1 p-3" aria-label="Admin navigation">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={() => setDrawerOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition-colors ${
                isActive
                  ? 'bg-accent text-accent-fg shadow-sm'
                  : 'text-ink-secondary hover:bg-subtle hover:text-ink'
              }`
            }
          >
            <item.icon className="h-4 w-4 shrink-0" />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="space-y-1 border-t border-line p-3">
        <div className="px-3 py-1.5 text-[11px] font-medium text-ink-muted">
          {t('common.usdBaseNote')}
        </div>
        <Link
          to="/"
          className="flex items-center gap-2.5 rounded-xl px-3.5 py-2 text-sm font-semibold text-ink-secondary transition-colors hover:bg-subtle hover:text-ink"
        >
          <Store className="h-4 w-4" />
          <span>{t('nav.viewStorefront')}</span>
        </Link>
        <button
          type="button"
          onClick={handleLogout}
          className="flex w-full items-center gap-2.5 rounded-xl px-3.5 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger-soft/60"
        >
          <LogOut className="h-4 w-4" />
          <span>{t('nav.logout')}</span>
        </button>
      </div>
    </div>
  );

  return (
    <div className="flex min-h-screen bg-canvas text-ink transition-colors">
      {/* Desktop Sidebar */}
      <aside className="hidden w-64 shrink-0 border-e border-line bg-surface lg:block">
        {SidebarContent}
      </aside>

      {/* Mobile Drawer */}
      {drawerOpen && (
        <div
          className="fixed inset-0 z-50 flex bg-black/50 backdrop-blur-sm lg:hidden"
          onClick={() => setDrawerOpen(false)}
        >
          <aside
            className="h-full w-72 border-e border-line bg-surface shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            {SidebarContent}
          </aside>
        </div>
      )}

      {/* Main Content */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between gap-3 border-b border-line bg-surface/95 px-4 backdrop-blur-md md:px-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setDrawerOpen(true)}
              aria-label={t('common.menu')}
              className="rounded-xl border border-line bg-surface p-2 text-ink hover:bg-subtle lg:hidden"
            >
              <Menu className="h-5 w-5" />
            </button>

            <Badge variant={connected ? 'success' : 'neutral'}>
              <span
                className={`h-2 w-2 rounded-full ${
                  connected ? 'bg-success' : 'bg-ink-muted'
                }`}
              />
              {connected ? t('nav.liveAlertsActive') : t('nav.connecting')}
            </Badge>
          </div>

          <div className="flex items-center gap-2.5">
            {adminAlerts.length > 0 && (
              <button
                type="button"
                onClick={clearAdminAlerts}
                className="inline-flex items-center gap-1.5 rounded-xl border border-warning/25 bg-warning-soft px-3 py-1.5 text-xs font-bold text-warning transition hover:opacity-90"
                title={t('nav.clearAlertsHint')}
              >
                <Bell className="h-3.5 w-3.5" />
                <span>{t('nav.newAlerts', { count: formatNumber(adminAlerts.length) })}</span>
              </button>
            )}

            <ThemeAndLangControls showCurrency={false} />

            <span
              dir="ltr"
              className="hidden max-w-[180px] truncate text-xs font-semibold text-ink-secondary sm:inline"
            >
              {user?.email}
            </span>
          </div>
        </header>

        <main className="flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}