package com.example.lumoo.service;

import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.order.CartRepository;
import com.example.lumoo.domain.order.CartService;
import com.example.lumoo.domain.product.Product;
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
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @InjectMocks private CartService cartService;

    private User buyer;
    private Product product;

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setName("Hat");
        product.setPrice(50.0);
    }

    // ── getTotal ──────────────────────────────────────────────────────────────

    @Test
    void getTotal_calculatesCorrectSum() {
        CartItem a = item(50.0, 2);
        CartItem b = item(25.0, 4);

        assertEquals(200.0, cartService.getTotal(List.of(a, b)), 0.001);
    }

    @Test
    void getTotal_returnsZero_whenEmpty() {
        assertEquals(0.0, cartService.getTotal(List.of()), 0.001);
    }

    // ── addItem ───────────────────────────────────────────────────────────────

    @Test
    void addItem_createsNewCartItem_whenNotInCart() {
        when(cartRepository.findByUserAndProduct(buyer, product)).thenReturn(Optional.empty());

        cartService.addItem(buyer, product);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartRepository).save(captor.capture());
        CartItem saved = captor.getValue();
        assertEquals(1, saved.getQuantity());
        assertEquals("Hat", saved.getName());
        assertEquals(50.0, saved.getPrice(), 0.001);
    }

    @Test
    void addItem_incrementsQuantity_whenAlreadyInCart() {
        CartItem existing = item(50.0, 2);
        when(cartRepository.findByUserAndProduct(buyer, product)).thenReturn(Optional.of(existing));

        cartService.addItem(buyer, product);

        assertEquals(3, existing.getQuantity());
        verify(cartRepository).save(existing);
    }

    // ── removeItem ────────────────────────────────────────────────────────────

    @Test
    void removeItem_returnsFalse_whenNotFound() {
        when(cartRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(cartService.removeItem(99L, buyer));
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void removeItem_returnsFalse_whenDifferentUser() {
        User other = new User();
        other.setId(99L);
        CartItem ci = item(50.0, 1);
        ci.setUser(buyer);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(ci));

        assertFalse(cartService.removeItem(1L, other));
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void removeItem_returnsTrue_andDeletes() {
        CartItem ci = item(50.0, 1);
        ci.setUser(buyer);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(ci));

        assertTrue(cartService.removeItem(1L, buyer));
        verify(cartRepository).delete(ci);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CartItem item(double price, int qty) {
        CartItem ci = new CartItem();
        ci.setPrice(price);
        ci.setQuantity(qty);
        return ci;
    }
}
