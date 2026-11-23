package com.example.ecommerce.config;

import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        User admin = ensureAdminExists();

        if (productRepository.count() > 0) {
            log.info("DataSeeder skipped: products already exist.");
            return;
        }

        log.info("Seeding initial NovaCart categories, products, variants, and coupons...");

        Category electronics = getOrCreateCategory(
                "Electronics",
                "Flagship audio, mechanical keyboards, and smart tech essentials.",
                "https://images.unsplash.com/photo-1498049794561-7780e7231661?auto=format&fit=crop&w=900&q=80"
        );
        Category fashion = getOrCreateCategory(
                "Fashion",
                "Tailored outerwear, minimalist apparel, and performance footwear.",
                "https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80"
        );
        Category accessories = getOrCreateCategory(
                "Accessories",
                "Precision timepieces, full-grain leather bags, and everyday carry.",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80"
        );
        Category homeOffice = getOrCreateCategory(
                "Home & Office",
                "Ergonomic workspace furniture and ambient lighting.",
                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=900&q=80"
        );

        // 1. Electronics - Headphones (Stock = 24)
        saveProduct(
                "Sony WH-1000XM5 Wireless ANC Headphones",
                "Industry-leading noise cancellation with dual processors, 30-hour battery life, multipoint Bluetooth 5.2, and crystal-clear hands-free calling.",
                electronics,
                admin,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80",
                List.of(
                        new VariantSeed("NC-AUD-XM5-BLK", "Over-Ear", "Midnight Black", "349.99", 18,
                                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-AUD-XM5-SLV", "Over-Ear", "Platinum Silver", "359.99", 6,
                                "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=900&q=80")
                )
        );

        // 2. Electronics - Mechanical Keyboard (Low Stock = 7 -> triggers Low Stock Alert)
        saveProduct(
                "Keychron Q1 Pro Wireless Mechanical Keyboard",
                "CNC machined aluminum 75% custom mechanical keyboard with QMK/VIA support, hot-swappable tactile switches, and double-gasket design.",
                electronics,
                admin,
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=900&q=80",
                List.of(
                        new VariantSeed("NC-KEY-Q1-BRN", "75% Layout", "Carbon Black", "199.00", 4,
                                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-KEY-Q1-WHT", "75% Layout", "Shell White", "209.00", 3,
                                "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=900&q=80")
                )
        );

        // 3. Fashion - Trench Coat (Stock = 26)
        saveProduct(
                "Urban Merino Wool Trench Coat",
                "Tailored double-breasted overcoat crafted from breathable Italian merino wool blend with water-repellent finish.",
                fashion,
                admin,
                "https://images.unsplash.com/photo-1539533018447-63fcce2678e3?auto=format&fit=crop&w=900&q=80",
                List.of(
                        new VariantSeed("NC-FSH-TRN-M-CAM", "M", "Camel", "229.00", 12,
                                "https://images.unsplash.com/photo-1539533018447-63fcce2678e3?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-FSH-TRN-L-BLK", "L", "Charcoal Black", "229.00", 9,
                                "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-FSH-TRN-XL-BLK", "XL", "Charcoal Black", "239.00", 5, null)
                )
        );

        // 4. Fashion - Running Sneakers (Stock = 29)
        saveProduct(
                "Apex Stride Knit Running Sneakers",
                "Ultra-lightweight engineered mesh upper paired with responsive nitrogen-infused foam cushioning for all-day urban comfort.",
                fashion,
                admin,
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
                List.of(
                        new VariantSeed("NC-FSH-SNK-42-RED", "EU 42", "Crimson", "139.00", 15,
                                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-FSH-SNK-43-WHT", "EU 43", "Cloud White", "145.00", 4,
                                "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-FSH-SNK-44-BLK", "EU 44", "Stealth Black", "139.00", 10, null)
                )
        );

        // 5. Accessories - Luxury Watch (Low Stock = 5 -> triggers Low Stock Alert)
        saveProduct(
                "Chronos Minimalist Sapphire Watch",
                "40mm brushed 316L stainless steel case with scratch-resistant sapphire crystal, Japanese automatic movement, and 5ATM water resistance.",
                accessories,
                admin,
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80",
                List.of(
                        new VariantSeed("NC-ACC-WTC-SLV", "40mm", "Brushed Silver", "279.00", 3,
                                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-ACC-WTC-BLK", "40mm", "Obsidian Black", "299.00", 2,
                                "https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=900&q=80")
                )
        );

        // 6. Accessories - Leather Backpack (Stock = 25)
        saveProduct(
                "Heritage Full-Grain Leather Backpack",
                "Handcrafted vegetable-tanned leather commuter backpack featuring a padded 16-inch laptop sleeve and brass hardware.",
                accessories,
                admin,
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=80",
                List.of(
                        new VariantSeed("NC-ACC-BAG-BRN", "20L", "Chestnut Brown", "185.00", 14,
                                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-ACC-BAG-BLK", "20L", "Jet Black", "185.00", 11,
                                "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=900&q=80")
                )
        );

        // 7. Home & Office - Ergonomic Chair (Low Stock = 8 -> triggers Low Stock Alert)
        saveProduct(
                "ErgoFlow Pro Mesh Office Chair",
                "Dynamic lumbar support system, breathable Korean elastomesh, 4D adjustable armrests, and synchronized tilt lock for marathon productivity.",
                homeOffice,
                admin,
                "https://images.unsplash.com/photo-1580480055273-228ff5388ef8?auto=format&fit=crop&w=900&q=80",
                List.of(
                        new VariantSeed("NC-HOM-CHR-GRY", "Standard", "Slate Gray", "389.00", 4,
                                "https://images.unsplash.com/photo-1580480055273-228ff5388ef8?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-HOM-CHR-BLK", "Standard", "Graphite Black", "399.00", 4, null)
                )
        );

        // 8. Home & Office - Smart Desk Lamp (Stock = 38)
        saveProduct(
                "Nordic Halo Smart Desk Lamp",
                "Flicker-free high-CRI LED task lamp with stepless dimming, warm-to-cool color temperature control, and USB-C wireless charging base.",
                homeOffice,
                admin,
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80",
                List.of(
                        new VariantSeed("NC-HOM-LMP-WHT", "Desk", "Matte White", "79.00", 22,
                                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=80"),
                        new VariantSeed("NC-HOM-LMP-BLK", "Desk", "Anodized Black", "79.00", 16, null)
                )
        );

        seedCoupons();
        log.info("DataSeeder completed: 4 categories, 8 products, and 2 coupons seeded.");
    }

    private User ensureAdminExists() {
        return userRepository.findByEmail("admin@novacart.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("NovaAdmin")
                        .email("admin@novacart.com")
                        .password(passwordEncoder.encode("Admin@123"))
                        .role(Role.ADMIN)
                        .isEnabled(true)
                        .build()));
    }

    private Category getOrCreateCategory(String name, String description, String imageUrl) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .description(description)
                        .imageUrl(imageUrl)
                        .build()));
    }

    private void saveProduct(
            String name,
            String description,
            Category category,
            User seller,
            String primaryImageUrl,
            List<VariantSeed> variantSeeds) {

        BigDecimal minPrice = variantSeeds.stream()
                .map(v -> new BigDecimal(v.price()))
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        int totalStock = variantSeeds.stream()
                .mapToInt(VariantSeed::stock)
                .sum();

        Product product = Product.builder()
                .name(name)
                .description(description)
                .price(minPrice)
                .stockQuantity(totalStock)
                .imageUrl(primaryImageUrl)
                .category(category)
                .seller(seller)
                .isActive(true)
                .build();

        product.getImages().add(ProductImage.builder()
                .product(product)
                .imageUrl(primaryImageUrl)
                .altText(name)
                .displayOrder(0)
                .isPrimary(true)
                .build());

        int imgOrder = 1;
        for (VariantSeed vs : variantSeeds) {
            product.getVariants().add(ProductVariant.builder()
                    .product(product)
                    .sku(vs.sku())
                    .size(vs.size())
                    .color(vs.color())
                    .price(new BigDecimal(vs.price()))
                    .stockQuantity(vs.stock())
                    .imageUrl(vs.imageUrl())
                    .isActive(true)
                    .build());

            if (vs.imageUrl() != null && !vs.imageUrl().equals(primaryImageUrl)) {
                product.getImages().add(ProductImage.builder()
                        .product(product)
                        .imageUrl(vs.imageUrl())
                        .altText(name + " - " + vs.color())
                        .displayOrder(imgOrder++)
                        .isPrimary(false)
                        .build());
            }
        }

        productRepository.save(product);
    }

    private void seedCoupons() {
        if (!couponRepository.existsByCodeIgnoreCase("SAVE20")) {
            couponRepository.save(Coupon.builder()
                    .code("SAVE20")
                    .description("20% off orders over $50 (capped at $60)")
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20.00"))
                    .minOrderAmount(new BigDecimal("50.00"))
                    .maxDiscountAmount(new BigDecimal("60.00"))
                    .startsAt(LocalDateTime.now().minusDays(1))
                    .expiresAt(LocalDateTime.now().plusYears(1))
                    .usageLimit(1000)
                    .perUserLimit(5)
                    .isActive(true)
                    .build());
        }

        if (!couponRepository.existsByCodeIgnoreCase("WELCOME15")) {
            couponRepository.save(Coupon.builder()
                    .code("WELCOME15")
                    .description("$15 flat discount on orders over $75")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(new BigDecimal("15.00"))
                    .minOrderAmount(new BigDecimal("75.00"))
                    .maxDiscountAmount(null)
                    .startsAt(LocalDateTime.now().minusDays(1))
                    .expiresAt(LocalDateTime.now().plusYears(1))
                    .usageLimit(1000)
                    .perUserLimit(1)
                    .isActive(true)
                    .build());
        }
    }

    private record VariantSeed(
            String sku,
            String size,
            String color,
            String price,
            int stock,
            String imageUrl
    ) {}
}