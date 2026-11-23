import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, Pencil, Upload, X, FolderPlus, Search, Package } from 'lucide-react';
import toast from 'react-hot-toast';
import { api } from '../../lib/axios';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { ProductImage } from '../../components/ui/ProductImageFallback';
import { TableRowsSkeleton } from '../../components/ui/Skeletons';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { fieldBaseClass } from '../../components/ui/Input';
import { formatNumber, getApiErrorMessage } from '../../lib/utils';
import { CategoryResponse, PagedResponse, ProductResponse } from '../../types/api';

interface VariantForm {
  id?: number;
  sku: string;
  size: string;
  color: string;
  price: string;
  stockQuantity: string;
  imageUrl: string;
}

interface ImageForm {
  imageUrl: string;
  altText: string;
  displayOrder: number;
  isPrimary: boolean;
}

export function AdminProductsPage() {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const { t } = useI18n();
  const { formatBasePrice } = useCurrency();

  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [stockFilter, setStockFilter] = useState<'all' | 'low' | 'out'>(
    searchParams.get('stock') === 'low' ? 'low' : 'all'
  );

  const [deletingProduct, setDeletingProduct] = useState<ProductResponse | null>(null);

  const [showCatModal, setShowCatModal] = useState(false);
  const [catName, setCatName] = useState('');
  const [catDesc, setCatDesc] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [price, setPrice] = useState('');
  const [stockQuantity, setStockQuantity] = useState('');
  const [images, setImages] = useState<ImageForm[]>([]);
  const [variants, setVariants] = useState<VariantForm[]>([]);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setKeyword(searchInput);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await api.get<CategoryResponse[]>('/api/categories')).data,
  });

  const { data: productsPage, isLoading } = useQuery({
    queryKey: ['admin-products', { keyword, categoryFilter, stockFilter, page }],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (keyword.trim()) params.set('keyword', keyword.trim());
      if (categoryFilter) params.set('categoryId', categoryFilter);
      params.set('page', stockFilter === 'all' ? String(page) : '0');
      params.set('size', stockFilter === 'all' ? '10' : '100');
      params.set('sort', 'createdAt,desc');
      return (await api.get<PagedResponse<ProductResponse>>(`/api/products/filter?${params}`)).data;
    },
  });

  const createCategoryMutation = useMutation({
    mutationFn: async () =>
      (await api.post('/api/categories', { name: catName, description: catDesc })).data,
    onSuccess: () => {
      toast.success(t('admin.catCreated'));
      setCatName('');
      setCatDesc('');
      setShowCatModal(false);
      queryClient.invalidateQueries({ queryKey: ['categories'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const saveProductMutation = useMutation({
    mutationFn: async () => {
      const payload = {
        name,
        description,
        price: Number(price),
        stockQuantity: Number(stockQuantity),
        imageUrl: images.find((i) => i.isPrimary)?.imageUrl || images[0]?.imageUrl || '',
        categoryId: Number(categoryId),
        images,
        variants: variants.map((v) => ({
          id: v.id,
          sku: v.sku,
          size: v.size,
          color: v.color,
          price: Number(v.price),
          stockQuantity: Number(v.stockQuantity),
          imageUrl: v.imageUrl || undefined,
        })),
      };
      if (editingId) {
        return (await api.put(`/api/products/${editingId}`, payload)).data;
      }
      return (await api.post('/api/products', payload)).data;
    },
    onSuccess: () => {
      toast.success(editingId ? t('admin.productUpdated') : t('admin.productCreated'));
      closeModal();
      queryClient.invalidateQueries({ queryKey: ['admin-products'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      queryClient.invalidateQueries({ queryKey: ['admin-low-stock'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const deleteProductMutation = useMutation({
    mutationFn: async (id: number) => api.delete(`/api/products/${id}`),
    onSuccess: () => {
      toast.success(t('admin.productDeleted'));
      setDeletingProduct(null);
      queryClient.invalidateQueries({ queryKey: ['admin-products'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      queryClient.invalidateQueries({ queryKey: ['admin-low-stock'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err, t('common.errorFallback'))),
  });

  const openCreateModal = () => {
    setEditingId(null);
    setName('');
    setDescription('');
    setCategoryId(categories[0]?.id ? String(categories[0].id) : '');
    setPrice('29.99');
    setStockQuantity('50');
    setImages([]);
    setVariants([
      {
        sku: `SKU-${Math.random().toString(36).slice(2, 8).toUpperCase()}`,
        size: 'M',
        color: 'Black',
        price: '29.99',
        stockQuantity: '50',
        imageUrl: '',
      },
    ]);
    setShowModal(true);
  };

  const openEditModal = (p: ProductResponse) => {
    setEditingId(p.id);
    setName(p.name);
    setDescription(p.description || '');
    setCategoryId(String(p.categoryId));
    setPrice(String(p.price));
    setStockQuantity(String(p.stockQuantity));
    setImages(
      p.images.length > 0
        ? p.images.map((img, idx) => ({
            imageUrl: img.imageUrl,
            altText: img.altText || '',
            displayOrder: img.displayOrder ?? idx,
            isPrimary: Boolean(img.primary ?? img.isPrimary),
          }))
        : p.imageUrl
        ? [{ imageUrl: p.imageUrl, altText: p.name, displayOrder: 0, isPrimary: true }]
        : []
    );
    setVariants(
      p.variants.map((v) => ({
        id: v.id,
        sku: v.sku,
        size: v.size || '',
        color: v.color || '',
        price: String(v.price),
        stockQuantity: String(v.stockQuantity),
        imageUrl: v.imageUrl || '',
      }))
    );
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingId(null);
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const { data } = await api.post<{ url: string }>('/api/images/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setImages((prev) => [
        ...prev,
        {
          imageUrl: data.url,
          altText: name || 'Product image',
          displayOrder: prev.length,
          isPrimary: prev.length === 0,
        },
      ]);
      toast.success(t('admin.imageUploaded'));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t('common.errorFallback')));
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  const rawProducts = productsPage?.content ?? [];
  const products =
    stockFilter === 'low'
      ? rawProducts.filter((p) => p.stockQuantity <= 10)
      : stockFilter === 'out'
      ? rawProducts.filter((p) => p.stockQuantity === 0)
      : rawProducts;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-ink">{t('admin.productsTitle')}</h1>
          <p className="mt-1 text-sm text-ink-secondary">{t('admin.productsSubtitle')}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => setShowCatModal(true)}>
            <FolderPlus className="h-4 w-4" />
            <span>{t('admin.newCategory')}</span>
          </Button>
          <Button onClick={openCreateModal}>
            <Plus className="h-4 w-4" />
            <span>{t('admin.newProduct')}</span>
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="grid grid-cols-1 gap-3 rounded-2xl border border-line bg-surface p-4 shadow-card sm:grid-cols-3">
        <div className="relative">
          <Search className="absolute start-3 top-3 h-4 w-4 text-ink-muted" />
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder={t('admin.searchProductsPlaceholder')}
            className={`${fieldBaseClass} ps-9`}
          />
        </div>

        <select
          aria-label={t('admin.categoryLabel')}
          value={categoryFilter}
          onChange={(e) => {
            setCategoryFilter(e.target.value);
            setPage(0);
          }}
          className={fieldBaseClass}
        >
          <option value="">{t('admin.allCategories')}</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>

        <select
          aria-label={t('admin.totalStockCol')}
          value={stockFilter}
          onChange={(e) => {
            const val = e.target.value as 'all' | 'low' | 'out';
            setStockFilter(val);
            if (val === 'all') {
              searchParams.delete('stock');
            } else {
              searchParams.set('stock', val);
            }
            setSearchParams(searchParams, { replace: true });
            setPage(0);
          }}
          className={fieldBaseClass}
        >
          <option value="all">{t('admin.stockFilterAll')}</option>
          <option value="low">{t('admin.stockFilterLow')}</option>
          <option value="out">{t('admin.stockFilterOut')}</option>
        </select>
      </div>

      {/* Products Table */}
      <div className="overflow-x-auto rounded-2xl border border-line bg-surface shadow-card">
        <table className="w-full text-start text-sm">
          <thead className="border-b border-line bg-subtle/70 text-xs uppercase text-ink-secondary">
            <tr>
              <th className="px-5 py-3.5 text-start">{t('admin.productCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.categoryCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.basePriceCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.totalStockCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.variantsCol')}</th>
              <th className="px-5 py-3.5 text-start">{t('admin.statusCol')}</th>
              <th className="px-5 py-3.5 text-end">{t('common.actions')}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {isLoading ? (
              <TableRowsSkeleton cols={7} rows={6} />
            ) : products.length === 0 ? (
              <tr>
                <td colSpan={7} className="p-6">
                  <EmptyState compact icon={Package} title={t('admin.noProductsMatch')} />
                </td>
              </tr>
            ) : (
              products.map((p) => {
                const isActive = p.active ?? p.isActive ?? true;
                return (
                  <tr key={p.id} className="transition-colors hover:bg-subtle/50">
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        <div className="h-11 w-11 shrink-0 overflow-hidden rounded-lg bg-subtle">
                          <ProductImage src={p.imageUrl} alt={p.name} iconClassName="h-5 w-5" />
                        </div>
                        <div className="min-w-0">
                          <p dir="auto" className="max-w-[240px] truncate font-bold text-ink">
                            {p.name}
                          </p>
                          <p dir="ltr" className="text-xs text-ink-muted">
                            ID #{formatNumber(p.id)}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-ink-secondary">{p.categoryName}</td>
                    <td dir="ltr" className="px-5 py-3.5 font-semibold text-ink">
                      {formatBasePrice(p.price)}
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge
                        variant={
                          p.stockQuantity === 0
                            ? 'danger'
                            : p.stockQuantity <= 10
                            ? 'warning'
                            : 'success'
                        }
                      >
                        <span dir="ltr">{formatNumber(p.stockQuantity)}</span>
                      </Badge>
                    </td>
                    <td dir="ltr" className="px-5 py-3.5 text-ink-secondary">
                      {formatNumber(p.variants?.length ?? 0)}
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge variant={isActive ? 'success' : 'neutral'}>
                        {isActive ? t('admin.activeStatus') : t('admin.inactiveStatus')}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5 text-end">
                      <div className="inline-flex gap-1">
                        <button
                          type="button"
                          onClick={() => openEditModal(p)}
                          title={t('admin.editTooltip')}
                          aria-label={t('admin.editTooltip')}
                          className="rounded-lg p-2 text-ink-secondary transition hover:bg-subtle hover:text-ink"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeletingProduct(p)}
                          title={t('admin.deleteTooltip')}
                          aria-label={t('admin.deleteTooltip')}
                          className="rounded-lg p-2 text-ink-secondary transition hover:bg-danger-soft hover:text-danger"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {stockFilter === 'all' && (productsPage?.totalPages ?? 0) > 1 && (
        <div className="flex justify-end gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            {t('common.previous')}
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= (productsPage?.totalPages ?? 0)}
            onClick={() => setPage((p) => p + 1)}
          >
            {t('common.next')}
          </Button>
        </div>
      )}

      {/* Category Modal */}
      {showCatModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm"
        >
          <div className="w-full max-w-md rounded-2xl border border-line bg-surface p-6 shadow-2xl">
            <h2 className="text-lg font-bold text-ink">{t('admin.createCategory')}</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                createCategoryMutation.mutate();
              }}
              className="mt-4 space-y-4"
            >
              <input
                required
                placeholder={t('admin.catNamePlaceholder')}
                value={catName}
                onChange={(e) => setCatName(e.target.value)}
                className={fieldBaseClass}
              />
              <textarea
                placeholder={t('admin.catDescPlaceholder')}
                value={catDesc}
                onChange={(e) => setCatDesc(e.target.value)}
                className={fieldBaseClass}
              />
              <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => setShowCatModal(false)}>
                  {t('common.cancel')}
                </Button>
                <Button type="submit" disabled={createCategoryMutation.isPending}>
                  {t('common.save')}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Structured Create/Edit Product Modal */}
      {showModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm"
        >
          <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-2xl border border-line bg-surface p-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-line pb-4">
              <h2 className="text-lg font-extrabold text-ink">
                {editingId ? t('admin.editProduct') : t('admin.createProduct')}
              </h2>
              <button
                type="button"
                aria-label={t('common.close')}
                onClick={closeModal}
                className="rounded-lg p-1.5 text-ink-muted hover:bg-subtle hover:text-ink"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                saveProductMutation.mutate();
              }}
              className="mt-5 space-y-6"
            >
              {/* Section 1: Basic Info */}
              <div className="rounded-xl border border-line bg-subtle/40 p-4">
                <h3 className="mb-3 text-xs font-bold uppercase tracking-wider text-ink-secondary">
                  {t('admin.basicInfoSection')}
                </h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-ink-secondary">
                      {t('admin.productName')}
                    </label>
                    <input
                      required
                      dir="auto"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      className={fieldBaseClass}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-ink-secondary">
                      {t('admin.categoryLabel')}
                    </label>
                    <select
                      required
                      value={categoryId}
                      onChange={(e) => setCategoryId(e.target.value)}
                      className={fieldBaseClass}
                    >
                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-ink-secondary">
                      {t('admin.basePriceLabel')}
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      dir="ltr"
                      required
                      value={price}
                      onChange={(e) => setPrice(e.target.value)}
                      className={fieldBaseClass}
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-ink-secondary">
                      {t('admin.defaultStockLabel')}
                    </label>
                    <input
                      type="number"
                      min="0"
                      dir="ltr"
                      required
                      value={stockQuantity}
                      onChange={(e) => setStockQuantity(e.target.value)}
                      className={fieldBaseClass}
                    />
                  </div>
                </div>

                <div className="mt-4">
                  <label className="mb-1 block text-xs font-semibold text-ink-secondary">
                    {t('admin.descriptionLabel')}
                  </label>
                  <textarea
                    rows={2}
                    dir="auto"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className={fieldBaseClass}
                  />
                </div>
              </div>

              {/* Section 2: Cloudinary Images */}
              <div className="rounded-xl border border-line bg-subtle/40 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-ink-secondary">
                    {t('admin.imagesLabel')}
                  </h3>
                  <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-xl bg-accent px-3 py-1.5 text-xs font-bold text-accent-fg hover:bg-accent-hover">
                    <Upload className="h-3.5 w-3.5" />
                    <span>{uploading ? t('admin.uploading') : t('admin.uploadImage')}</span>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={handleImageUpload}
                      disabled={uploading}
                      className="hidden"
                    />
                  </label>
                </div>

                {images.length > 0 && (
                  <div className="mt-3 flex flex-wrap gap-3">
                    {images.map((img, idx) => (
                      <div
                        key={idx}
                        className={`relative rounded-xl border-2 bg-surface p-1.5 ${
                          img.isPrimary ? 'border-accent' : 'border-line'
                        }`}
                      >
                        <img src={img.imageUrl} alt="" className="h-20 w-20 rounded-lg object-cover" />
                        <button
                          type="button"
                          onClick={() =>
                            setImages((prev) =>
                              prev.map((item, i) => ({ ...item, isPrimary: i === idx }))
                            )
                          }
                          className="mt-1 block w-full text-center text-[10px] font-bold text-accent"
                        >
                          {img.isPrimary ? t('admin.primary') : t('admin.setPrimary')}
                        </button>
                        <button
                          type="button"
                          aria-label={t('common.delete')}
                          onClick={() => setImages((prev) => prev.filter((_, i) => i !== idx))}
                          className="absolute -end-2 -top-2 rounded-full bg-danger p-1 text-white shadow"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Section 3: Responsive Variants */}
              <div className="rounded-xl border border-line bg-subtle/40 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-ink-secondary">
                    {t('admin.variantsLabel')}
                  </h3>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setVariants((prev) => [
                        ...prev,
                        {
                          sku: `SKU-${Math.random().toString(36).slice(2, 8).toUpperCase()}`,
                          size: '',
                          color: '',
                          price: price || '10.00',
                          stockQuantity: '10',
                          imageUrl: '',
                        },
                      ])
                    }
                  >
                    <Plus className="h-3.5 w-3.5" />
                    <span>{t('admin.addVariant')}</span>
                  </Button>
                </div>

                <div className="mt-3 space-y-3">
                  {variants.map((v, idx) => (
                    <div
                      key={idx}
                      className="grid grid-cols-1 gap-2.5 rounded-xl border border-line bg-surface p-3 sm:grid-cols-2 lg:grid-cols-6"
                    >
                      <input
                        required
                        dir="ltr"
                        placeholder={t('admin.skuPlaceholder')}
                        value={v.sku}
                        onChange={(e) =>
                          setVariants((prev) =>
                            prev.map((item, i) => (i === idx ? { ...item, sku: e.target.value } : item))
                          )
                        }
                        className={fieldBaseClass}
                      />
                      <input
                        placeholder={t('admin.sizePlaceholder')}
                        value={v.size}
                        onChange={(e) =>
                          setVariants((prev) =>
                            prev.map((item, i) => (i === idx ? { ...item, size: e.target.value } : item))
                          )
                        }
                        className={fieldBaseClass}
                      />
                      <input
                        placeholder={t('admin.colorPlaceholder')}
                        value={v.color}
                        onChange={(e) =>
                          setVariants((prev) =>
                            prev.map((item, i) => (i === idx ? { ...item, color: e.target.value } : item))
                          )
                        }
                        className={fieldBaseClass}
                      />
                      <input
                        type="number"
                        step="0.01"
                        dir="ltr"
                        required
                        placeholder={t('admin.pricePlaceholder')}
                        value={v.price}
                        onChange={(e) =>
                          setVariants((prev) =>
                            prev.map((item, i) => (i === idx ? { ...item, price: e.target.value } : item))
                          )
                        }
                        className={fieldBaseClass}
                      />
                      <input
                        type="number"
                        min="0"
                        dir="ltr"
                        required
                        placeholder={t('admin.stockPlaceholder')}
                        value={v.stockQuantity}
                        onChange={(e) =>
                          setVariants((prev) =>
                            prev.map((item, i) =>
                              i === idx ? { ...item, stockQuantity: e.target.value } : item
                            )
                          )
                        }
                        className={fieldBaseClass}
                      />
                      <Button
                        variant="danger"
                        size="sm"
                        aria-label={t('common.delete')}
                        onClick={() => setVariants((prev) => prev.filter((_, i) => i !== idx))}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex justify-end gap-2 border-t border-line pt-4">
                <Button variant="outline" onClick={closeModal}>
                  {t('common.cancel')}
                </Button>
                <Button type="submit" disabled={saveProductMutation.isPending}>
                  {saveProductMutation.isPending
                    ? t('admin.savingProduct')
                    : t('admin.saveProduct')}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={deletingProduct !== null}
        title={t('admin.confirmDeleteProductTitle')}
        description={t('admin.confirmDeleteProductDesc', {
          name: deletingProduct?.name ?? '',
        })}
        loading={deleteProductMutation.isPending}
        onCancel={() => setDeletingProduct(null)}
        onConfirm={() => {
          if (deletingProduct) deleteProductMutation.mutate(deletingProduct.id);
        }}
      />
    </div>
  );
}