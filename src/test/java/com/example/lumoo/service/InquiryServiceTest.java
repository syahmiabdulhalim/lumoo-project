package com.example.lumoo.service;

import com.example.lumoo.domain.inquiry.Inquiry;
import com.example.lumoo.domain.inquiry.InquiryRepository;
import com.example.lumoo.domain.inquiry.InquiryService;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock private InquiryRepository inquiryRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private InquiryService inquiryService;

    @Test
    void send_savesInquiry_whenProductExists() {
        Product product = new Product();
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        inquiryService.send(1L, "Ali", "ali@test.com", "Product Inquiry", "Is this available?");

        ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(captor.capture());
        Inquiry saved = captor.getValue();
        assertEquals("Ali", saved.getBuyerName());
        assertEquals("ali@test.com", saved.getEmail());
        assertEquals("Is this available?", saved.getMessage());
        assertEquals(product, saved.getProduct());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void send_doesNothing_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        inquiryService.send(99L, "Ali", "ali@test.com", "Hello", "Hello");

        verify(inquiryRepository, never()).save(any());
    }

    @Test
    void delete_callsDeleteById() {
        inquiryService.delete(5L);

        verify(inquiryRepository).deleteById(5L);
    }

    @Test
    void getAll_delegatesToRepo() {
        inquiryService.getAll();

        verify(inquiryRepository).findAllByOrderByCreatedAtDesc();
    }
}
