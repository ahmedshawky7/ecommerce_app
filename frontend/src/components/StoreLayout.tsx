import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import { useI18n } from '../context/I18nContext';

export function StoreLayout() {
  const { t } = useI18n();

  return (
    <div className="flex min-h-screen flex-col bg-canvas text-ink transition-colors">
      <Navbar />
      <main className="w-full flex-1">
        <Outlet />
      </main>
      <footer className="w-full border-t border-line bg-surface py-8 transition-colors">
        <div className="flex w-full flex-col items-center justify-between gap-4 px-4 text-sm text-ink-secondary sm:flex-row md:px-6">
          <p className="font-semibold text-ink">{t('nav.footerTitle')}</p>
          <p>{t('nav.footerSub')}</p>
        </div>
      </footer>
    </div>
  );
}