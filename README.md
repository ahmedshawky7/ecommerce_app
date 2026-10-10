# 🛍️ E-Commerce Platform

A **full-stack e-commerce platform** built with Spring Boot 4 and Flutter Web, featuring a unified admin panel and customer store in a single application.

[![Live Demo](https://img.shields.io/badge/Live%20Demo-Online-success)](https://ecommerceapp-production-4a9c.up.railway.app)
[![API Docs](https://img.shields.io/badge/API-Swagger-blue)](https://ecommerceapp-production-5c76.up.railway.app/swagger-ui.html)

---

## ✨ Features

### 🛒 Customer Features
- **Authentication** — JWT with refresh tokens
- **Product Browsing** — Grid layout, search, category filter
- **Product Details** — Images, description, stock, seller info
- **Shopping Cart** — Add, update, remove items
- **Checkout** — Shipping info + **Stripe Payment**
- **Order History** — View past orders, cancel pending ones
- **Profile Management** — User info, logout

### 🔐 Admin Features
- **Dashboard** — Real-time stats (Revenue, Orders, Customers, Products)
- **Orders Management** — View all, filter, update status
- **Users Management** — List, search, toggle active status
- **Analytics** — Daily sales chart, revenue by category
- **Product Management** — Create, update, delete

### 🎯 Smart Routing
Single app detects user role and routes:
- **ADMIN** → Admin Panel
- **CUSTOMER** → Store

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 17, Spring Boot 4, Spring Security |
| **Database** | MySQL 8 |
| **Cache** | Redis |
| **Message Queue** | RabbitMQ (CloudAMQP) |
| **Storage** | Cloudinary |
| **Payment** | Stripe Checkout |
| **Auth** | JWT + Refresh Tokens |
| **Frontend** | Flutter Web (Bloc/Cubit) |
| **API Docs** | Swagger / OpenAPI 3 |
| **Deployment** | Railway (Backend + Frontend), CloudAMQP |

---

## 📸 Screenshots

### Admin Dashboard
![Admin Dashboard](docs/screenshots/admin-dashboard.png)

### Customer Shop
![Customer Shop](docs/screenshots/customer-shop.png)

### Checkout with Stripe
![Stripe Checkout](docs/screenshots/stripe-checkout.png)

### Payment Success
![Payment Success](docs/screenshots/payment-success.png)

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven
- Flutter 3.47+
- Docker (for MySQL, Redis, RabbitMQ)

### 1. Clone Repository

\`\`\`bash
git clone https://github.com/ahmedshawky7/ecommerce_app.git
cd ecommerce_app
\`\`\`

### 2. Start Infrastructure

\`\`\`bash
docker-compose up -d
\`\`\`

### 3. Configure Backend

Create `backend/.env`:
\`\`\`env
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your_base64_secret

MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
\`\`\`

### 4. Run Backend

\`\`\`bash
cd backend
./mvnw spring-boot:run
\`\`\`

Backend runs on `http://localhost:8080`.

### 5. Run Frontend

\`\`\`bash
cd frontend_admin
flutter pub get
flutter run -d chrome
\`\`\`

---

## 🧪 Test Stripe Payment

1. Register as a customer
2. Add products to cart
3. Proceed to checkout
4. Use test card: `4242 4242 4242 4242`
5. Any future date, any CVC

---

## 📁 Project Structure

\`\`\`
ecommerce_app/
├── backend/                          # Spring Boot API
│   ├── src/main/java/com/example/ecommerce/
│   │   ├── config/                   # Configs (Security, Redis, RabbitMQ, etc.)
│   │   ├── controller/               # REST Controllers
│   │   ├── dto/                      # Data Transfer Objects
│   │   ├── entity/                   # JPA Entities
│   │   ├── exception/                # Global Exception Handler
│   │   ├── repository/               # Spring Data Repositories
│   │   ├── security/                 # JWT + Security
│   │   └── service/                  # Business Logic
│   └── src/main/resources/
│       └── application.properties
│
└── frontend_admin/                   # Flutter Web (Admin + Customer)
    ├── lib/
    │   ├── core/                     # Shared utilities
    │   │   ├── constants/
    │   │   ├── network/
    │   │   ├── storage/
    │   │   ├── theme/
    │   │   └── widgets/
    │   └── features/
    │       ├── auth/                 # Login + Register
    │       ├── dashboard/            # Admin Dashboard
    │       ├── orders/               # Admin Orders
    │       ├── users/                # Admin Users
    │       ├── analytics/            # Admin Analytics
    │       ├── products/             # Customer Products
    │       ├── cart/                 # Customer Cart
    │       ├── checkout/             # Stripe Checkout
    │       ├── customer_orders/      # Customer Orders
    │       └── profile/              # Customer Profile
    └── web/
\`\`\`

---

## 🌐 API Endpoints

### Auth
- `POST /api/auth/register` — Register
- `POST /api/auth/login` — Login
- `POST /api/auth/refresh` — Refresh Token

### Products & Categories
- `GET /api/products` — List products
- `GET /api/products/{id}` — Product details
- `GET /api/products/search?keyword=` — Search
- `GET /api/categories` — List categories

### Cart
- `GET /api/cart` — Get cart
- `POST /api/cart/items` — Add item
- `PUT /api/cart/items/{id}` — Update item
- `DELETE /api/cart/items/{id}` — Remove item

### Orders
- `POST /api/orders/create-checkout-session` — Stripe checkout
- `GET /api/orders` — User orders
- `PUT /api/orders/{id}/cancel` — Cancel order

### Admin
- `GET /api/admin/dashboard/stats`
- `GET /api/admin/orders`
- `GET /api/admin/users`
- `GET /api/admin/analytics/sales`

**Full API docs:** [Swagger UI](https://ecommerceapp-production-5c76.up.railway.app/swagger-ui.html)

---

## 🔐 Security Notes

- All secrets are stored as **environment variables** (never committed)
- JWT tokens expire after 15 minutes
- Refresh tokens expire after 7 days
- Stripe webhooks verified via signature

---

## 👨‍💻 Author

**Ahmed Shawky**
- GitHub: [@ahmedshawky7](https://github.com/ahmedshawky7)
- Email: ahmedeltabakh703@gmail.com

---

## 📝 License

This project is licensed under the MIT License.
