package com.example.lumoo.service;

import com.example.lumoo.domain.subscriber.SubscriberService;
import com.example.lumoo.domain.subscriber.Subscriber;
import com.example.lumoo.domain.subscriber.SubscriberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriberServiceTest {

    @Mock private SubscriberRepository repo;
    @InjectMocks private SubscriberService service;

    // ── Validation ───────────────────────────────────────────────────────────

    @Test
    void subscribe_null_returnsInvalid() {
        assertEquals(SubscriberService.Result.INVALID, service.subscribe(null));
    }

    @Test
    void subscribe_noAtSign_returnsInvalid() {
        assertEquals(SubscriberService.Result.INVALID, service.subscribe("notanemail"));
    }

    @Test
    void subscribe_tooLong_returnsInvalid() {
        String longEmail = "a".repeat(250) + "@x.com"; // > 254 chars
        assertEquals(SubscriberService.Result.INVALID, service.subscribe(longEmail));
    }

    // ── Happy path ───────────────────────────────────────────────────────────

    @Test
    void subscribe_newEmail_savesAndReturnsSubscribed() {
        when(repo.findByEmail("user@example.com")).thenReturn(Optional.empty());

        SubscriberService.Result result = service.subscribe("user@example.com");

        assertEquals(SubscriberService.Result.SUBSCRIBED, result);
        verify(repo).save(any(Subscriber.class));
    }

    @Test
    void subscribe_existingActiveEmail_returnsAlreadySubscribed() {
        Subscriber existing = new Subscriber();
        existing.setActive(true);
        when(repo.findByEmail("user@example.com")).thenReturn(Optional.of(existing));

        assertEquals(SubscriberService.Result.ALREADY_SUBSCRIBED, service.subscribe("user@example.com"));
        verify(repo, never()).save(any());
    }

    @Test
    void subscribe_reactivatesInactiveSubscriber() {
        Subscriber existing = new Subscriber();
        existing.setActive(false);
        when(repo.findByEmail("user@example.com")).thenReturn(Optional.of(existing));

        SubscriberService.Result result = service.subscribe("user@example.com");

        assertEquals(SubscriberService.Result.SUBSCRIBED, result);
        assertTrue(existing.isActive());
        verify(repo).save(existing);
    }

    @Test
    void subscribe_normalisesEmailToLowerCase() {
        when(repo.findByEmail("user@example.com")).thenReturn(Optional.empty());

        service.subscribe("  USER@EXAMPLE.COM  ");

        // Should save normalised email
        ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);
        verify(repo).save(captor.capture());
        assertEquals("user@example.com", captor.getValue().getEmail());
    }
}
