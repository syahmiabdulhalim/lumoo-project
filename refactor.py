#!/usr/bin/env python3
"""
Package-by-domain refactoring script for lumoo-project.
Moves all Java files to their new domain locations and rewrites
package declarations + imports in every file (main + test).
"""
import os, re, shutil

BASE      = "src/main/java/com/example/lumoo"
TEST_BASE = "src/test/java/com/example/lumoo"

# ── File moves: old relative path → new relative path ────────────────────────
MOVES = {
    # Product domain
    "model/Product.java":                      "domain/product/Product.java",
    "repository/ProductRepository.java":       "domain/product/ProductRepository.java",
    "service/ProductService.java":             "domain/product/ProductService.java",
    "controller/CategoryController.java":      "domain/product/CategoryController.java",
    "dto/ProductForm.java":                    "domain/product/ProductForm.java",
    "model/Review.java":                       "domain/product/Review.java",
    "repository/ReviewRepository.java":        "domain/product/ReviewRepository.java",
    "service/ReviewService.java":              "domain/product/ReviewService.java",

    # Order domain
    "model/Order.java":                        "domain/order/Order.java",
    "model/OrderItem.java":                    "domain/order/OrderItem.java",
    "repository/OrderRepository.java":         "domain/order/OrderRepository.java",
    "repository/OrderItemRepository.java":     "domain/order/OrderItemRepository.java",
    "service/OrderService.java":               "domain/order/OrderService.java",
    "controller/OrderController.java":         "domain/order/OrderController.java",
    "model/CartItem.java":                     "domain/order/CartItem.java",
    "repository/CartRepository.java":          "domain/order/CartRepository.java",
    "service/CartService.java":                "domain/order/CartService.java",
    "controller/CartController.java":          "domain/order/CartController.java",
    "controller/InvoiceController.java":       "domain/order/InvoiceController.java",

    # Payment domain
    "model/WebhookEvent.java":                 "domain/payment/WebhookEvent.java",
    "repository/WebhookEventRepository.java":  "domain/payment/WebhookEventRepository.java",
    "service/ModemPayService.java":            "domain/payment/ModemPayService.java",
    "controller/PaymentController.java":       "domain/payment/PaymentController.java",

    # User domain
    "model/User.java":                         "domain/user/User.java",
    "model/Role.java":                         "domain/user/Role.java",
    "model/Notification.java":                 "domain/user/Notification.java",
    "repository/UserRepository.java":          "domain/user/UserRepository.java",
    "repository/NotificationRepository.java":  "domain/user/NotificationRepository.java",
    "service/UserService.java":                "domain/user/UserService.java",
    "service/CustomUserDetailsService.java":   "domain/user/CustomUserDetailsService.java",
    "controller/AuthController.java":          "domain/user/AuthController.java",
    "controller/ProfileController.java":       "domain/user/ProfileController.java",
    "controller/ForgotPasswordController.java":"domain/user/ForgotPasswordController.java",
    "dto/RegisterRequest.java":                "domain/user/RegisterRequest.java",
    "dto/AuthRequest.java":                    "domain/user/AuthRequest.java",

    # Vendor domain
    "model/VendorApplication.java":            "domain/vendor/VendorApplication.java",
    "repository/VendorApplicationRepository.java":"domain/vendor/VendorApplicationRepository.java",
    "service/VendorApplicationService.java":   "domain/vendor/VendorApplicationService.java",
    "controller/VendorApplicationController.java":"domain/vendor/VendorApplicationController.java",
    "controller/VendorController.java":        "domain/vendor/VendorController.java",

    # Blog domain
    "model/BlogPost.java":                     "domain/blog/BlogPost.java",
    "repository/BlogPostRepository.java":      "domain/blog/BlogPostRepository.java",
    "service/BlogService.java":                "domain/blog/BlogService.java",
    "controller/BlogController.java":          "domain/blog/BlogController.java",
    "controller/AdminBlogController.java":     "domain/blog/AdminBlogController.java",

    # Admin domain
    "controller/AdminController.java":         "domain/admin/AdminController.java",
    "model/SiteSettings.java":                 "domain/admin/SiteSettings.java",
    "repository/SiteSettingsRepository.java":  "domain/admin/SiteSettingsRepository.java",
    "service/SiteSettingsService.java":        "domain/admin/SiteSettingsService.java",
    "config/SiteSettingsInterceptor.java":     "domain/admin/SiteSettingsInterceptor.java",

    # Inquiry domain
    "model/Inquiry.java":                      "domain/inquiry/Inquiry.java",
    "repository/InquiryRepository.java":       "domain/inquiry/InquiryRepository.java",
    "service/InquiryService.java":             "domain/inquiry/InquiryService.java",
    "controller/InquiryController.java":       "domain/inquiry/InquiryController.java",

    # Subscriber domain
    "model/Subscriber.java":                   "domain/subscriber/Subscriber.java",
    "repository/SubscriberRepository.java":    "domain/subscriber/SubscriberRepository.java",
    "service/SubscriberService.java":          "domain/subscriber/SubscriberService.java",

    # PDPP domain
    "model/AuditLog.java":                     "domain/pdpp/AuditLog.java",
    "repository/AuditLogRepository.java":      "domain/pdpp/AuditLogRepository.java",
    "service/AuditService.java":               "domain/pdpp/AuditService.java",
    "model/ErasureRequest.java":               "domain/pdpp/ErasureRequest.java",
    "repository/ErasureRequestRepository.java":"domain/pdpp/ErasureRequestRepository.java",
    "model/DataAccessRequest.java":            "domain/pdpp/DataAccessRequest.java",
    "repository/DataAccessRequestRepository.java":"domain/pdpp/DataAccessRequestRepository.java",
    "model/BreachIncident.java":               "domain/pdpp/BreachIncident.java",
    "repository/BreachIncidentRepository.java":"domain/pdpp/BreachIncidentRepository.java",
    "service/CustomerRightsService.java":      "domain/pdpp/CustomerRightsService.java",
    "service/DataBreachService.java":          "domain/pdpp/DataBreachService.java",
    "service/DataRetentionService.java":       "domain/pdpp/DataRetentionService.java",
    "controller/CustomerRightsController.java":"domain/pdpp/CustomerRightsController.java",

    # Infrastructure — Security
    "security/DataEncryptor.java":             "infrastructure/security/DataEncryptor.java",
    "security/EncryptedStringConverter.java":  "infrastructure/security/EncryptedStringConverter.java",
    "security/SpringContextHolder.java":       "infrastructure/security/SpringContextHolder.java",
    "security/RateLimitInterceptor.java":      "infrastructure/security/RateLimitInterceptor.java",
    "config/SecurityConfig.java":              "infrastructure/security/SecurityConfig.java",
    "config/CustomSuccessHandler.java":        "infrastructure/security/CustomSuccessHandler.java",

    # Infrastructure — Email
    "service/EmailService.java":               "infrastructure/email/EmailService.java",

    # Infrastructure — Web
    "config/WebMvcConfig.java":                "infrastructure/web/WebMvcConfig.java",
    "config/LocaleConfig.java":                "infrastructure/web/LocaleConfig.java",

    # App bootstrap
    "config/DataInitializer.java":             "app/DataInitializer.java",
    "config/DatabaseSeeder.java":              "app/DatabaseSeeder.java",

    # Shared
    "dto/ApiResponse.java":                    "shared/dto/ApiResponse.java",
    "exception/GlobalExceptionHandler.java":   "shared/exception/GlobalExceptionHandler.java",
    "exception/ResourceNotFoundException.java":"shared/exception/ResourceNotFoundException.java",
    "exception/UnauthorizedException.java":    "shared/exception/UnauthorizedException.java",
    "exception/ValidationException.java":      "shared/exception/ValidationException.java",
    "controller/SitemapController.java":       "shared/SitemapController.java",
    "controller/WebController.java":           "shared/WebController.java",
}

# ── Build class → new package lookup ─────────────────────────────────────────
CLASS_PKG = {}
for src_rel, dst_rel in MOVES.items():
    cls = os.path.basename(src_rel).replace(".java", "")
    parts = dst_rel.replace(".java", "").split("/")[:-1]
    CLASS_PKG[cls] = "com.example.lumoo." + ".".join(parts)

# ── Build old-package → [classes] for wildcard expansion ─────────────────────
OLD_PKG_CLASSES: dict[str, list[str]] = {}
for src_rel in MOVES:
    cls  = os.path.basename(src_rel).replace(".java", "")
    dirs = src_rel.split("/")[:-1]
    if dirs:
        old_pkg = "com.example.lumoo." + ".".join(dirs)
        OLD_PKG_CLASSES.setdefault(old_pkg, []).append(cls)

# ── Rewrite a single file's package + imports ─────────────────────────────────
def rewrite(path: str, new_pkg: str):
    with open(path, "r") as f:
        content = f.read()

    # 1. Update package declaration
    content = re.sub(
        r"^package com\.example\.lumoo[^;]*;",
        f"package {new_pkg};",
        content, count=1, flags=re.MULTILINE
    )

    # 2. Expand wildcard imports → specific imports (already in new pkg paths)
    for old_pkg, classes in OLD_PKG_CLASSES.items():
        wildcard = f"import {old_pkg}.*;"
        if wildcard in content:
            specific = "\n".join(
                f"import {CLASS_PKG[c]}.{c};"
                for c in classes if c in CLASS_PKG
            )
            content = content.replace(wildcard, specific)

    # 3. Update every explicit import of a moved class
    for cls, new_class_pkg in CLASS_PKG.items():
        content = re.sub(
            rf"import com\.example\.lumoo\.[a-z./]+\.{re.escape(cls)};",
            f"import {new_class_pkg}.{cls};",
            content
        )

    with open(path, "w") as f:
        f.write(content)

# ── Step 1: Copy files to new locations ──────────────────────────────────────
print("=== Copying files to new locations ===")
for src_rel, dst_rel in MOVES.items():
    src = os.path.join(BASE, src_rel)
    dst = os.path.join(BASE, dst_rel)
    if not os.path.exists(src):
        print(f"  SKIP (not found): {src_rel}")
        continue
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy2(src, dst)
    print(f"  {src_rel} → {dst_rel}")

# ── Step 2: Rewrite package + imports in all new files ───────────────────────
print("\n=== Rewriting package declarations and imports ===")
for dst_rel in MOVES.values():
    dst = os.path.join(BASE, dst_rel)
    if not os.path.exists(dst):
        continue
    parts = dst_rel.replace(".java", "").split("/")[:-1]
    new_pkg = "com.example.lumoo." + ".".join(parts)
    rewrite(dst, new_pkg)
    print(f"  Rewritten: {dst_rel}  →  package {new_pkg}")

# ── Step 3: Update imports in test files (don't move them) ───────────────────
print("\n=== Updating test file imports ===")
for root, _, files in os.walk(TEST_BASE):
    for fname in files:
        if not fname.endswith(".java"):
            continue
        fpath = os.path.join(root, fname)
        # Determine test file's own package (don't change it)
        rel = os.path.relpath(fpath, TEST_BASE).replace(".java", "").replace(os.sep, ".")
        pkg = "com.example.lumoo." + rel.rsplit(".", 1)[0] if "." in rel else "com.example.lumoo"
        rewrite(fpath, pkg)
        print(f"  Updated imports: {os.path.relpath(fpath, TEST_BASE)}")

# ── Step 4: Delete original files ────────────────────────────────────────────
print("\n=== Removing old source files ===")
for src_rel, dst_rel in MOVES.items():
    src = os.path.join(BASE, src_rel)
    dst = os.path.join(BASE, dst_rel)
    if os.path.exists(src) and os.path.abspath(src) != os.path.abspath(dst):
        os.remove(src)
        print(f"  Deleted: {src_rel}")

# ── Step 5: Remove now-empty old directories ─────────────────────────────────
print("\n=== Cleaning empty directories ===")
for old_dir in ["model", "repository", "service", "controller", "config", "dto", "exception", "security"]:
    p = os.path.join(BASE, old_dir)
    if os.path.isdir(p):
        remaining = os.listdir(p)
        if not remaining:
            os.rmdir(p)
            print(f"  Removed empty dir: {old_dir}/")
        else:
            print(f"  WARNING {old_dir}/ still has: {remaining}")

print("\nDone. Run: ./mvnw compile")
