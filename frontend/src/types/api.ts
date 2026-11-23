export type Role = 'CUSTOMER' | 'ADMIN';

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';

export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT';

export interface AuthResponse {
  token: string;
  refreshToken: string;
  email: string;
  username: string;
  role: Role;
  id: number;
}

export interface PagedResponse<T> {
  content: T[];
  page?: number;
  number?: number; // Spring Data Page uses "number"
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description?: string;
  imageUrl?: string;
  createdAt: string;
  productCount: number;
}

export interface ProductVariantResponse {
  id: number;
  sku: string;
  size?: string;
  color?: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string;
  active?: boolean;
  isActive?: boolean;
}

export interface ProductImageResponse {
  id: number;
  imageUrl: string;
  altText?: string;
  displayOrder: number;
  primary?: boolean;
  isPrimary?: boolean;
}

export interface ProductResponse {
  id: number;
  name: string;
  description?: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string;
  averageRating: number;
  reviewCount: number;
  active?: boolean;
  isActive?: boolean;
  categoryId: number;
  categoryName: string;
  sellerId: number;
  sellerName: string;
  variants: ProductVariantResponse[];
  images: ProductImageResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CartItemResponse {
  id: number;
  productId: number;
  variantId: number;
  sku: string;
  size?: string;
  color?: string;
  productName: string;
  productImageUrl?: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  availableStock: number;
}

export interface CartResponse {
  id: number;
  items: CartItemResponse[];
  totalItems: number;
  totalPrice: number;
}

export interface PricingBreakdownResponse {
  subtotal: number;
  discountAmount: number;
  shippingAmount: number;
  taxAmount: number;
  totalAmount: number;
  appliedCouponCode?: string | null;
}

export interface OrderItemResponse {
  id: number;
  productId: number;
  variantId?: number | null;
  sku?: string;
  variantLabel?: string;
  productName: string;
  productImageUrl?: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderResponse {
  id: number;
  orderNumber: string;
  subtotalAmount: number;
  discountAmount: number;
  shippingAmount: number;
  taxAmount: number;
  couponCode?: string | null;
  totalAmount: number;
  status: OrderStatus;
  shippingAddress: string;
  phone: string;
  paymentMethod: string;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface AdminOrderResponse extends OrderResponse {
  paymentIntentId?: string;
  customerId: number;
  customerName: string;
  customerEmail: string;
}

export interface CheckoutResponse {
  orderId: number;
  orderNumber: string;
  clientSecret?: string | null;
  subtotalAmount: number;
  discountAmount: number;
  shippingAmount: number;
  taxAmount: number;
  totalAmount: number;
  couponCode?: string | null;
}

export interface CheckoutSessionResponse {
  sessionId?: string | null;
  sessionUrl: string;
  orderId: number;
  orderNumber: string;
}

export interface ReviewResponse {
  id: number;
  productId: number;
  userId: number;
  username: string;
  rating: number;
  title?: string;
  comment: string;
  verifiedPurchase: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AddressResponse {
  id: number;
  label: string;
  fullName: string;
  phone: string;
  streetAddress: string;
  city: string;
  state?: string;
  postalCode?: string;
  country: string;
  isDefault: boolean;
  createdAt: string;
}

export interface WishlistItemResponse {
  id: number;
  productId: number;
  productName: string;
  imageUrl?: string;
  price: number;
  averageRating: number;
  reviewCount: number;
  stockQuantity: number;
  categoryName: string;
  addedAt: string;
}

export interface CouponResponse {
  id: number;
  code: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscountAmount?: number | null;
  startsAt?: string | null;
  expiresAt: string;
  usageLimit?: number | null;
  usageCount: number;
  perUserLimit: number;
  isActive: boolean;
  createdAt: string;
}

export interface DashboardStatsResponse {
  totalRevenue: number;
  totalOrders: number;
  totalCustomers: number;
  totalProducts: number;
  pendingOrders: number;
  confirmedOrders: number;
  cancelledOrders: number;
}

export interface TopSellingProductResponse {
  productId: number;
  productName: string;
  productImageUrl?: string;
  totalQuantitySold: number;
  totalRevenue: number;
}

export interface LowStockProductResponse {
  productId: number;
  productName: string;
  stockQuantity: number;
  price: number;
}

export interface SalesAnalyticsResponse {
  date: string;
  ordersCount: number;
  revenue: number;
}

export interface RevenueByCategoryResponse {
  categoryId: number;
  categoryName: string;
  totalQuantitySold: number;
  totalRevenue: number;
}

export interface UserManagementResponse {
  id: number;
  username: string;
  email: string;
  role: Role;
  isEnabled: boolean;
  createdAt: string;
}

export interface AuditLogResponse {
  id: number;
  actorEmail: string;
  actorRole: string;
  action: string;
  entityType: string;
  entityId?: number;
  oldValue?: string;
  newValue?: string;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface OrderStatusEvent {
  orderId: number;
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
  message: string;
  timestamp: string;
}

export interface AdminAlertEvent {
  type: string;
  title: string;
  message: string;
  referenceId: number;
  timestamp: string;
}