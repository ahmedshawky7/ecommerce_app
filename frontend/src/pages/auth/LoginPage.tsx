import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, Store } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../../context/AuthContext';
import { useI18n } from '../../context/I18nContext';
import { ThemeAndLangControls } from '../../components/ui/ThemeAndLangControls';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { getApiErrorMessage } from '../../lib/utils';

export function LoginPage() {
  const { login } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const user = await login(email, password);
      toast.success(t('auth.welcomeBack', { name: user.username }));
      navigate(user.role === 'ADMIN' ? '/admin' : '/', { replace: true });
    } catch (err) {
      toast.error(getApiErrorMessage(err, t('auth.invalidCredentials')));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-canvas px-4 py-12">
      <div className="absolute end-4 top-4 sm:end-6 sm:top-6">
        <ThemeAndLangControls showCurrency={false} />
      </div>

      <div className="w-full max-w-md rounded-2xl border border-line bg-surface p-7 shadow-card sm:p-8">
        <div className="mb-6 flex flex-col items-center text-center">
          <Link
            to="/"
            className="flex h-12 w-12 items-center justify-center rounded-2xl bg-accent text-accent-fg shadow-sm"
          >
            <Store className="h-6 w-6" />
          </Link>
          <h1 className="mt-4 text-2xl font-extrabold text-ink">{t('auth.signInTitle')}</h1>
          <p className="mt-1 text-sm text-ink-secondary">{t('auth.signInSubtitle')}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            id="login-email"
            type="email"
            required
            dir="ltr"
            label={t('auth.email')}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
          />

          <Input
            id="login-password"
            type="password"
            required
            dir="ltr"
            label={t('auth.password')}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />

          <Button type="submit" fullWidth size="lg" disabled={loading}>
            {loading ? t('auth.signingIn') : t('auth.signInBtn')}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-ink-secondary">
          {t('auth.noAccount')}{' '}
          <Link to="/register" className="font-semibold text-accent hover:underline">
            {t('auth.createOne')}
          </Link>
        </p>

        <div className="mt-6 flex items-center justify-center gap-1.5 border-t border-line pt-4 text-xs text-ink-muted">
          <ShieldCheck className="h-3.5 w-3.5 text-success" />
          <span>{t('auth.secureBadge')}</span>
        </div>
      </div>
    </div>
  );
}