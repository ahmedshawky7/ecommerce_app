import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  BarChart,
  Bar,
} from 'recharts';
import {
  DollarSign,
  ShoppingBag,
  Users,
  Package,
  AlertTriangle,
  BarChart3,
  ArrowRight,
} from 'lucide-react';
import { api } from '../../lib/axios';
import { useTheme } from '../../context/ThemeContext';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { ProductImage } from '../../components/ui/ProductImageFallback';
import { Badge } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { formatNumber } from '../../lib/utils';
import {
  DashboardStatsResponse,
  LowStockProductResponse,
  RevenueByCategoryResponse,
  SalesAnalyticsResponse,
  TopSellingProductResponse,
} from '../../types/api';

export function AdminOverviewPage() {
  const { isDark } = useTheme();
  const { t } = useI18n();
  const { formatBasePrice } = useCurrency();

  const { data: stats } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: async () => (await api.get<DashboardStatsResponse>('/api/admin/dashboard/stats')).data,
  });

  const { data: dailySales = [] } = useQuery({
    queryKey: ['admin-daily-sales'],
    queryFn: async () =>
      (await api.get<SalesAnalyticsResponse[]>('/api/admin/analytics/sales?days=14')).data,
  });

  const { data: categoryRevenue = [] } = useQuery({
    queryKey: ['admin-category-revenue'],
    queryFn: async () =>
      (await api.get<RevenueByCategoryResponse[]>('/api/admin/analytics/revenue-by-category')).data,
  });

  const { data: topProducts = [] } = useQuery({
    queryKey: ['admin-top-products'],
    queryFn: async () =>
      (await api.get<TopSellingProductResponse[]>('/api/admin/dashboard/top-products?limit=5')).data,
  });

  const { data: lowStock = [] } = useQuery({
    queryKey: ['admin-low-stock'],
    queryFn: async () =>
      (
        await api.get<LowStockProductResponse[]>(
          '/api/admin/inventory/low-stock?threshold=10&limit=10'
        )
      ).data,
  });

  const kpis = [
    {
      label: t('admin.totalRevenue'),
      value: formatBasePrice(stats?.totalRevenue ?? 0),
      icon: DollarSign,
      iconCls: 'bg-success-soft text-success dark:bg-success/15',
    },
    {
      label: t('admin.totalOrders'),
      value: formatNumber(stats?.totalOrders ?? 0),
      sub: t('admin.ordersSub', {
        pending: formatNumber(stats?.pendingOrders ?? 0),
        confirmed: formatNumber(stats?.confirmedOrders ?? 0),
      }),
      icon: ShoppingBag,
      iconCls: 'bg-accent-soft text-accent dark:bg-accent/15',
    },
    {
      label: t('admin.customers'),
      value: formatNumber(stats?.totalCustomers ?? 0),
      icon: Users,
      iconCls: 'bg-subtle text-ink-secondary',
    },
    {
      label: t('admin.products'),
      value: formatNumber(stats?.totalProducts ?? 0),
      icon: Package,
      iconCls: 'bg-warning-soft text-warning dark:bg-warning/15',
    },
  ];

  const gridStroke = isDark ? '#2B3242' : '#E2E5EB';
  const tickColor = isDark ? '#8892A6' : '#6B7280';
  const primaryChartColor = isDark ? '#3B82F6' : '#2563EB';
  const secondaryChartColor = isDark ? '#14B8A6' : '#0D9488';
  const tooltipStyle = isDark
    ? {
        backgroundColor: '#181C26',
        borderColor: '#2B3242',
        borderRadius: '12px',
        color: '#F3F4F6',
      }
    : {
        backgroundColor: '#FFFFFF',
        borderColor: '#E2E5EB',
        borderRadius: '12px',
        color: '#111827',
      };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-ink">{t('admin.overviewTitle')}</h1>
        <p className="mt-1 text-sm text-ink-secondary">{t('admin.overviewSubtitle')}</p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {kpis.map((kpi) => (
          <div
            key={kpi.label}
            className="rounded-2xl border border-line bg-surface p-5 shadow-card"
          >
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs font-semibold uppercase tracking-wide text-ink-secondary">
                {kpi.label}
              </span>
              <div className={`rounded-xl p-2.5 ${kpi.iconCls}`}>
                <kpi.icon className="h-5 w-5" />
              </div>
            </div>
            <p dir="ltr" className="mt-3 text-2xl font-extrabold tracking-tight text-ink">
              {kpi.value}
            </p>
            {kpi.sub && <p className="mt-1 text-xs text-ink-muted">{kpi.sub}</p>}
          </div>
        ))}
      </div>

      {/* High-Priority Low Stock Alert Section */}
      {lowStock.length > 0 && (
        <div className="rounded-2xl border border-warning/35 bg-surface p-5 shadow-card">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line pb-4">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-warning-soft text-warning dark:bg-warning/15">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-ink">{t('admin.lowStockTitle')}</h2>
                <p className="text-xs text-ink-secondary">{t('admin.lowStockSubtitle')}</p>
              </div>
            </div>
            <Link
              to="/admin/products?stock=low"
              className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-subtle px-3.5 py-2 text-xs font-bold text-ink transition hover:border-line-strong"
            >
              <span>{t('admin.manageInventory')}</span>
              <ArrowRight className="h-3.5 w-3.5 rtl:rotate-180" />
            </Link>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {lowStock.map((item) => (
              <div
                key={item.productId}
                className="flex items-center justify-between gap-3 rounded-xl border border-line bg-subtle/40 px-4 py-3 text-sm"
              >
                <div className="min-w-0">
                  <p dir="auto" className="truncate font-semibold text-ink">
                    {item.productName}
                  </p>
                  <p dir="ltr" className="text-xs text-ink-muted">
                    {formatBasePrice(item.price)}
                  </p>
                </div>
                <Badge variant={item.stockQuantity === 0 ? 'danger' : 'warning'}>
                  {item.stockQuantity === 0
                    ? t('admin.outOfStockLabel')
                    : t('admin.unitsLeft', { count: formatNumber(item.stockQuantity) })}
                </Badge>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Charts with Graceful Empty States */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-line bg-surface p-6 shadow-card">
          <h2 className="text-base font-bold text-ink">{t('admin.dailyRevenue')}</h2>
          {dailySales.length === 0 ? (
            <div className="mt-4">
              <EmptyState
                compact
                icon={BarChart3}
                title={t('admin.noChartDataTitle')}
                description={t('admin.noChartDataDesc')}
              />
            </div>
          ) : (
            <div className="mt-4 h-72" dir="ltr">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={dailySales}>
                  <defs>
                    <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={primaryChartColor} stopOpacity={0.28} />
                      <stop offset="95%" stopColor={primaryChartColor} stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke={gridStroke} />
                  <XAxis dataKey="date" tick={{ fontSize: 11, fill: tickColor }} stroke={gridStroke} />
                  <YAxis tick={{ fontSize: 11, fill: tickColor }} stroke={gridStroke} />
                  <Tooltip
                    contentStyle={tooltipStyle}
                    formatter={(val) => formatBasePrice(Number(val ?? 0))}
                  />
                  <Area
                    type="monotone"
                    dataKey="revenue"
                    stroke={primaryChartColor}
                    strokeWidth={2.5}
                    fill="url(#revGrad)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="rounded-2xl border border-line bg-surface p-6 shadow-card">
          <h2 className="text-base font-bold text-ink">{t('admin.revenueByCategory')}</h2>
          {categoryRevenue.length === 0 ? (
            <div className="mt-4">
              <EmptyState
                compact
                icon={BarChart3}
                title={t('admin.noChartDataTitle')}
                description={t('admin.noChartDataDesc')}
              />
            </div>
          ) : (
            <div className="mt-4 h-72" dir="ltr">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={categoryRevenue}>
                  <CartesianGrid strokeDasharray="3 3" stroke={gridStroke} />
                  <XAxis
                    dataKey="categoryName"
                    tick={{ fontSize: 11, fill: tickColor }}
                    stroke={gridStroke}
                  />
                  <YAxis tick={{ fontSize: 11, fill: tickColor }} stroke={gridStroke} />
                  <Tooltip
                    contentStyle={tooltipStyle}
                    formatter={(val) => formatBasePrice(Number(val ?? 0))}
                  />
                  <Bar dataKey="totalRevenue" fill={secondaryChartColor} radius={[8, 8, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>

      {/* Top Selling Products */}
      <div className="rounded-2xl border border-line bg-surface p-6 shadow-card">
        <h2 className="text-base font-bold text-ink">{t('admin.topSelling')}</h2>
        {topProducts.length === 0 ? (
          <div className="mt-4">
            <EmptyState compact icon={Package} title={t('admin.noSalesData')} />
          </div>
        ) : (
          <div className="mt-4 divide-y divide-line">
            {topProducts.map((item) => (
              <div key={item.productId} className="flex items-center justify-between gap-4 py-3">
                <div className="flex min-w-0 items-center gap-3">
                  <div className="h-11 w-11 shrink-0 overflow-hidden rounded-xl bg-subtle">
                    <ProductImage
                      src={item.productImageUrl}
                      alt={item.productName}
                      iconClassName="h-5 w-5"
                    />
                  </div>
                  <div className="min-w-0">
                    <p dir="auto" className="truncate text-sm font-semibold text-ink">
                      {item.productName}
                    </p>
                    <p className="text-xs text-ink-muted">
                      {t('admin.sold', { count: formatNumber(item.totalQuantitySold) })}
                    </p>
                  </div>
                </div>
                <span dir="ltr" className="shrink-0 text-sm font-bold text-ink">
                  {formatBasePrice(item.totalRevenue)}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}