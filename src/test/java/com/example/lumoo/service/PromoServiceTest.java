package com.example.lumoo.service;
import com.example.lumoo.domain.promo.PromoCode;
import com.example.lumoo.domain.promo.PromoCodeRepository;
import com.example.lumoo.domain.promo.PromoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class PromoServiceTest {
    @Mock private PromoCodeRepository repo;
    @InjectMocks private PromoService service;
    private PromoCode promo(PromoCode.DiscountType type, double value) {
        PromoCode p = new PromoCode();
        p.setId(1L);
        p.setCode("SAVE");
        p.setDiscountType(type);
        p.setDiscountValue(value);
        when(repo.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(p));
        return p;
    }
    @Test
    void redeem_takesOneUse_andReturnsDiscount() {
        promo(PromoCode.DiscountType.PERCENTAGE, 10);
        when(repo.incrementUsage(1L)).thenReturn(1);
        assertEquals(Optional.of(20.0), service.redeem(" SAVE ", 200));
        verify(repo).incrementUsage(1L);
    }
    @Test
    void redeem_failsWhenLastUseWasTakenConcurrently() {
        promo(PromoCode.DiscountType.FIXED, 50);
        when(repo.incrementUsage(1L)).thenReturn(0);
        assertTrue(service.redeem("SAVE", 200).isEmpty());
    }
    @Test
    void invalidCodes_areNotRedeemed() {
        PromoCode expired = promo(PromoCode.DiscountType.FIXED, 50);
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        assertTrue(service.redeem("SAVE", 200).isEmpty());
        expired.setExpiresAt(null);
        expired.setActive(false);
        assertTrue(service.redeem("SAVE", 200).isEmpty());
        expired.setActive(true);
        expired.setMinimumOrder(500);
        assertTrue(service.redeem("SAVE", 200).isEmpty());
        verify(repo, never()).incrementUsage(anyLong());
    }
    @Test
    void discountNeverExceedsItemsTotal() {
        promo(PromoCode.DiscountType.FIXED, 999);
        assertEquals(Optional.of(80.0), service.quote("SAVE", 80));
    }
    @Test
    void quote_doesNotTakeAUse() {
        promo(PromoCode.DiscountType.PERCENTAGE, 10);
        service.quote("SAVE", 100);
        verify(repo, never()).incrementUsage(anyLong());
    }
}
