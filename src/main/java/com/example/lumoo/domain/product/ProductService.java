package com.example.lumoo.domain.product;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
@Service
public class ProductService {
    @Autowired private ProductRepository productRepository;
    @Value("${app.upload.dir:/app/uploads/products}")
    private String uploadDir;
    public List<Product> getAll() {
        return productRepository.findAllWithVendor();
    }
    public org.springframework.data.domain.Page<Product> getAllPage(int page, int size) {
        return productRepository.findAll(
            org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("id").descending()));
    }
    @Cacheable(value = "products", key = "'all-approved'")
    public List<Product> getAllApprovedList() {
        return productRepository.findByApprovedTrueWithVendor();
    }
    public Page<Product> getAllApproved(Pageable pageable) {
        return productRepository.findByApprovedTrue(pageable);
    }
    public List<Product> searchApproved(String keyword) {
        return productRepository.findByApprovedTrueAndNameContainingIgnoreCaseWithVendor(keyword);
    }
    public Page<Product> searchApprovedPaged(String keyword, Pageable pageable) {
        try {
            Page<Product> result = productRepository.fullTextSearch(keyword + "*", pageable);
            if (!result.isEmpty()) return result;
        } catch (Exception ignored) {
        }
        return productRepository.findByApprovedTrueAndNameContainingIgnoreCasePaged(keyword, pageable);
    }
    public List<Product> searchByName(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }
    public List<Product> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    @Cacheable(value = "products", key = "'cat-' + #category.toLowerCase()")
    public List<Product> getApprovedByCategory(String category) {
        return productRepository.findByCategoryIgnoreCaseAndApprovedWithVendor(category);
    }
    public Page<Product> getApprovedByCategoryPaged(String category, Pageable pageable) {
        return productRepository.findByCategoryIgnoreCaseAndApprovedPaged(category, pageable);
    }
    public List<Product> getByVendor(User vendor) {
        return productRepository.findByVendor(vendor);
    }
    public List<Product> getPendingApproval() {
        return productRepository.findByApproved(false);
    }
    public List<Product> getPendingImageApproval() {
        return productRepository.findByApprovedTrueAndImageApprovedFalse();
    }
    public List<Product> getApprovedByVendor(User vendor) {
        return productRepository.findByVendorAndApprovedTrue(vendor);
    }
    public Page<Product> getApprovedByVendorPaged(User vendor, Pageable pageable) {
        return productRepository.findByVendorAndApprovedTrue(vendor, pageable);
    }
    public Page<Product> getApprovedByVendorAndCategoryPaged(User vendor, String category, Pageable pageable) {
        return productRepository.findByVendorAndApprovedTrueAndCategoryIgnoreCase(vendor, category, pageable);
    }
    public List<String> getDistinctCategoriesByVendor(User vendor) {
        return productRepository.findDistinctCategoriesByVendor(vendor);
    }
    public List<User> getVendorsWithProducts() {
        return productRepository.findDistinctVendorsWithApprovedProducts();
    }
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private void validateImage(MultipartFile file) throws IOException {
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct.toLowerCase())) {
            throw new IOException("Invalid file type. Only JPG, PNG and WEBP images are allowed.");
        }
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf(".")).toLowerCase();
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                throw new IOException("Invalid file extension.");
            }
        }
    }
    public String saveImage(MultipartFile file) throws IOException {
        validateImage(file);
        String extension = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf(".")).toLowerCase();
        }
        String filename = UUID.randomUUID() + extension;
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/products/" + filename;
    }
    @CacheEvict(value = "products", allEntries = true)
    public void addProduct(Product product, User vendor) {
        product.setVendor(vendor);
        product.setApproved(false);
        product.setImageApproved(false);
        productRepository.save(product);
    }
    @CacheEvict(value = "products", allEntries = true)
    public boolean updateProduct(Long id, Product updated, User vendor, MultipartFile image) throws IOException {
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null || !existing.getVendor().getId().equals(vendor.getId())) return false;
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPrice(updated.getPrice());
        boolean imageChanged = false;
        if (image != null && !image.isEmpty()) {
            existing.setImageUrl(saveImage(image));
            imageChanged = true;
        } else if (updated.getImageUrl() != null && !updated.getImageUrl().isBlank()
                && !updated.getImageUrl().equals(existing.getImageUrl())) {
            existing.setImageUrl(updated.getImageUrl());
            imageChanged = true;
        }
        if (imageChanged) existing.setImageApproved(false);
        productRepository.save(existing);
        return true;
    }
    @CacheEvict(value = "products", allEntries = true)
    public boolean deleteByVendor(Long id, User vendor) {
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null || !existing.getVendor().getId().equals(vendor.getId())) return false;
        productRepository.deleteById(id);
        return true;
    }
    @CacheEvict(value = "products", allEntries = true)
    public void approve(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setApproved(true);
            p.setImageApproved(true);
            productRepository.save(p);
        });
    }
    @CacheEvict(value = "products", allEntries = true)
    public void approveImage(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setImageApproved(true);
            productRepository.save(p);
        });
    }
    @CacheEvict(value = "products", allEntries = true)
    public void rejectImage(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setImageUrl(null);
            p.setImageApproved(false);
            productRepository.save(p);
        });
    }
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
    public boolean decrementStock(Long productId, int qty) {
        return productRepository.decrementStock(productId, qty) > 0;
    }
    public void returnStock(Long productId, int qty) {
        productRepository.returnStock(productId, qty);
    }
}
