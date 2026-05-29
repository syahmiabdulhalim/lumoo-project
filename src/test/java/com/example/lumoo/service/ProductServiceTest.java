package com.example.lumoo.service;

import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductRepository;
import com.example.lumoo.domain.product.ProductService;
import com.example.lumoo.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductService productService;

    private User vendor;
    private Product product;

    @BeforeEach
    void setUp() {
        vendor = new User();
        vendor.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setVendor(vendor);
        product.setName("Test Product");
    }

    // ── addProduct ────────────────────────────────────────────────────────────

    @Test
    void addProduct_setsVendorAndSaves() {
        Product p = new Product();
        productService.addProduct(p, vendor);

        assertEquals(vendor, p.getVendor());
        verify(productRepository).save(p);
    }

    @Test
    void addProduct_setsApprovedFalse() {
        Product p = new Product();
        productService.addProduct(p, vendor);

        assertFalse(p.isApproved());
        assertFalse(p.isImageApproved());
    }

    // ── approve / approveImage / rejectImage ──────────────────────────────────

    @Test
    void approve_setsApprovedAndImageApproved() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        productService.approve(10L);

        assertTrue(product.isApproved());
        assertTrue(product.isImageApproved());
        verify(productRepository).save(product);
    }

    @Test
    void approve_doesNothing_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        productService.approve(99L);

        verify(productRepository, never()).save(any());
    }

    @Test
    void approveImage_setsImageApproved() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        productService.approveImage(10L);

        assertTrue(product.isImageApproved());
        verify(productRepository).save(product);
    }

    @Test
    void rejectImage_clearsImageUrlAndSetsImageApprovedFalse() {
        product.setImageUrl("/uploads/products/photo.jpg");
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        productService.rejectImage(10L);

        assertNull(product.getImageUrl());
        assertFalse(product.isImageApproved());
        verify(productRepository).save(product);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_callsDeleteById() {
        productService.delete(10L);

        verify(productRepository).deleteById(10L);
    }

    // ── deleteByVendor ────────────────────────────────────────────────────────

    @Test
    void deleteByVendor_returnsFalse_whenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(productService.deleteByVendor(99L, vendor));
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void deleteByVendor_returnsFalse_whenDifferentVendor() {
        User other = new User();
        other.setId(99L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertFalse(productService.deleteByVendor(10L, other));
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void deleteByVendor_returnsTrue_andDeletes() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertTrue(productService.deleteByVendor(10L, vendor));
        verify(productRepository).deleteById(10L);
    }

    // ── query delegation ──────────────────────────────────────────────────────

    @Test
    void getAllApprovedList_delegatesToRepo() {
        when(productRepository.findByApprovedTrue()).thenReturn(List.of(product));

        List<Product> result = productService.getAllApprovedList();

        assertEquals(1, result.size());
        verify(productRepository).findByApprovedTrue();
    }

    @Test
    void searchApproved_delegatesToRepo() {
        when(productRepository.findByApprovedTrueAndNameContainingIgnoreCase("hat"))
                .thenReturn(List.of(product));

        List<Product> result = productService.searchApproved("hat");

        assertEquals(1, result.size());
    }

    @Test
    void getApprovedByCategory_delegatesToRepo() {
        when(productRepository.findByCategoryIgnoreCaseAndApproved("CLOTHES"))
                .thenReturn(List.of(product));

        List<Product> result = productService.getApprovedByCategory("CLOTHES");

        assertEquals(1, result.size());
    }

    @Test
    void findById_delegatesToRepo() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertTrue(productService.findById(10L).isPresent());
    }
}
