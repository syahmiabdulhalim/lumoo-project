package com.example.lumoo.service;

import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.payment.PayoutService;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @InjectMocks PayoutService payoutService;
    @Mock OrderRepository orderRepository;
    @Mock HttpClient mockHttpClient;
    @Mock @SuppressWarnings("rawtypes") HttpResponse mockResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        setField("objectMapper", new ObjectMapper());
        setField("httpClient", mockHttpClient);
        setField("apiKey", "sk_test_key");
        setField("apiUrl", "https://api.modempay.com/v1");
    }

    private void setField(String name, Object value) throws Exception {
        Field f = PayoutService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(payoutService, value);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Order order(long id, double earnings) {
        Order o = new Order();
        o.setId(id);
        o.setVendorEarnings(earnings);
        return o;
    }

    private Order orderWithVendor(long id, double earnings, String phone, String network) {
        User vendor = new User();
        vendor.setId(100L);
        vendor.setUsername("vendor1");
        vendor.setFullName("Vendor One");
        vendor.setPayoutPhone(phone);
        vendor.setPayoutNetwork(network);

        Product product = new Product();
        product.setVendor(vendor);

        OrderItem item = new OrderItem();
        item.setProduct(product);

        Order o = order(id, earnings);
        o.setItems(List.of(item));
        return o;
    }

    // ── isConfigured ──────────────────────────────────────────────────────────

    @Test
    void isConfigured_returnsTrue_whenApiKeySet() {
        assertTrue(payoutService.isConfigured());
    }

    @Test
    void isConfigured_returnsFalse_whenApiKeyBlank() throws Exception {
        setField("apiKey", "");
        assertFalse(payoutService.isConfigured());
    }

    // ── tryPayoutVendor — skip paths ──────────────────────────────────────────

    @Test
    void tryPayoutVendor_skips_whenNotConfigured() throws Exception {
        setField("apiKey", "");
        payoutService.tryPayoutVendor(order(1L, 100.0));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void tryPayoutVendor_skipped_whenNoItems() {
        Order o = order(1L, 100.0);
        o.setItems(List.of());
        payoutService.tryPayoutVendor(o);
        assertEquals("SKIPPED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void tryPayoutVendor_skipped_whenItemHasNoProduct() {
        OrderItem item = new OrderItem();
        Order o = order(1L, 100.0);
        o.setItems(List.of(item));
        payoutService.tryPayoutVendor(o);
        assertEquals("SKIPPED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void tryPayoutVendor_skipped_whenVendorHasNoPayoutPhone() {
        Order o = orderWithVendor(1L, 100.0, null, "afrimoney");
        payoutService.tryPayoutVendor(o);
        assertEquals("SKIPPED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void tryPayoutVendor_skipped_whenPayoutPhoneBlank() {
        Order o = orderWithVendor(1L, 100.0, "  ", "afrimoney");
        payoutService.tryPayoutVendor(o);
        assertEquals("SKIPPED", o.getPayoutStatus());
    }

    @Test
    void tryPayoutVendor_skipped_whenUnsupportedNetwork() {
        Order o = orderWithVendor(1L, 100.0, "7000001", "qmoney");
        payoutService.tryPayoutVendor(o);
        assertEquals("SKIPPED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void tryPayoutVendor_skipped_whenVendorNetworkNull() {
        Order o = orderWithVendor(1L, 100.0, "7000001", null);
        payoutService.tryPayoutVendor(o);
        assertEquals("SKIPPED", o.getPayoutStatus());
    }

    @Test
    void tryPayoutVendor_skipped_whenEarningsTooSmall() {
        Order o = orderWithVendor(1L, 0.4, "7000001", "afrimoney");
        payoutService.tryPayoutVendor(o);
        assertEquals("SKIPPED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    // ── tryPayoutVendor — HTTP paths ──────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void tryPayoutVendor_setPending_whenTransferSucceeds() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"id\":\"transfer-abc\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Order o = orderWithVendor(1L, 90.0, "7000001", "afrimoney");
        payoutService.tryPayoutVendor(o);

        assertEquals("PENDING", o.getPayoutStatus());
        assertEquals("transfer-abc", o.getPayoutTransferId());
        verify(orderRepository).save(o);
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryPayoutVendor_setPending_whenTransferReturns201() throws Exception {
        when(mockResponse.statusCode()).thenReturn(201);
        when(mockResponse.body()).thenReturn("{\"id\":\"transfer-xyz\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Order o = orderWithVendor(2L, 50.0, "7000002", "wave");
        payoutService.tryPayoutVendor(o);

        assertEquals("PENDING", o.getPayoutStatus());
        assertEquals("transfer-xyz", o.getPayoutTransferId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryPayoutVendor_setFailed_whenApiReturnsError() throws Exception {
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("{\"message\":\"Invalid account\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Order o = orderWithVendor(3L, 90.0, "7000001", "afrimoney");
        payoutService.tryPayoutVendor(o);

        assertEquals("FAILED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryPayoutVendor_setFailed_whenHttpThrows() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("network error"));

        Order o = orderWithVendor(4L, 90.0, "7000001", "afrimoney");
        payoutService.tryPayoutVendor(o);

        assertEquals("FAILED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    @SuppressWarnings("unchecked")
    void tryPayoutVendor_usesFullName_whenAvailable() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"id\":\"t1\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Order o = orderWithVendor(5L, 90.0, "7000001", "wave");
        payoutService.tryPayoutVendor(o);

        assertEquals("PENDING", o.getPayoutStatus());
    }

    // ── handleTransferWebhook ─────────────────────────────────────────────────

    @Test
    void handleTransferWebhook_setsPaid_onSucceeded() {
        Order o = order(1L, 90.0);
        when(orderRepository.findByPayoutTransferId("t-1")).thenReturn(Optional.of(o));

        payoutService.handleTransferWebhook("t-1", "transfer.succeeded");

        assertEquals("PAID", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void handleTransferWebhook_setsFailed_onFailed() {
        Order o = order(1L, 90.0);
        when(orderRepository.findByPayoutTransferId("t-2")).thenReturn(Optional.of(o));

        payoutService.handleTransferWebhook("t-2", "transfer.failed");

        assertEquals("FAILED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void handleTransferWebhook_setsFailed_onReversed() {
        Order o = order(1L, 90.0);
        when(orderRepository.findByPayoutTransferId("t-3")).thenReturn(Optional.of(o));

        payoutService.handleTransferWebhook("t-3", "transfer.reversed");

        assertEquals("FAILED", o.getPayoutStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void handleTransferWebhook_doesNothing_forUnknownEvent() {
        Order o = order(1L, 90.0);
        when(orderRepository.findByPayoutTransferId("t-4")).thenReturn(Optional.of(o));

        payoutService.handleTransferWebhook("t-4", "transfer.flagged");

        assertNull(o.getPayoutStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleTransferWebhook_doesNothing_whenTransferNotFound() {
        when(orderRepository.findByPayoutTransferId("unknown")).thenReturn(Optional.empty());

        payoutService.handleTransferWebhook("unknown", "transfer.succeeded");

        verify(orderRepository, never()).save(any());
    }
}
