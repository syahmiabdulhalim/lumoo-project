package com.example.lumoo.service;
import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.shipping.ShippingRate;
import com.example.lumoo.domain.shipping.ShippingRateRepository;
import com.example.lumoo.domain.shipping.ShippingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {
    @Mock private ShippingRateRepository repo;
    @InjectMocks private ShippingService service;
    private ShippingRate rate(String name, double base, double perKg, double maxKg) {
        ShippingRate r = new ShippingRate();
        r.setCourierName(name);
        r.setBaseRateGmd(base);
        r.setPerKgRateGmd(perKg);
        r.setMaxWeightKg(maxKg);
        return r;
    }
    @Test
    void quoteFor_selfCollect_isFree() {
        assertEquals(Optional.of(0.0), service.quoteFor("none", 5));
        assertEquals(Optional.of(0.0), service.quoteFor("", 5));
        assertEquals(Optional.of(0.0), service.quoteFor(null, 5));
        verifyNoInteractions(repo);
    }
    @Test
    void quoteFor_knownCourier_usesServerRate() {
        when(repo.findByActiveTrueOrderByDisplayOrderAscCourierNameAsc())
                .thenReturn(List.of(rate("Gamcel Express", 50, 10, 30)));
        assertEquals(Optional.of(80.0), service.quoteFor("gamcel express", 3));
    }
    @Test
    void quoteFor_unknownOrInactiveCourier_isEmpty() {
        when(repo.findByActiveTrueOrderByDisplayOrderAscCourierNameAsc()).thenReturn(List.of());
        assertTrue(service.quoteFor("Gamcel Express", 3).isEmpty());
    }
    @Test
    void quoteFor_weightOutOfRange_isEmpty() {
        when(repo.findByActiveTrueOrderByDisplayOrderAscCourierNameAsc())
                .thenReturn(List.of(rate("Gamcel Express", 50, 10, 30)));
        assertTrue(service.quoteFor("Gamcel Express", 31).isEmpty());
    }
    @Test
    void cartWeightKg_defaultsToOneKgPerUnit() {
        Product heavy = new Product();
        heavy.setWeightKg(2.5);
        CartItem a = new CartItem(); a.setProduct(heavy); a.setQuantity(2);
        CartItem b = new CartItem(); b.setQuantity(3);
        assertEquals(8.0, ShippingService.cartWeightKg(List.of(a, b)), 0.001);
    }
}
