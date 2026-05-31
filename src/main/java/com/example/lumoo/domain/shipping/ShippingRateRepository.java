package com.example.lumoo.domain.shipping;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {
    List<ShippingRate> findByActiveTrueOrderByDisplayOrderAscCourierNameAsc();
    List<ShippingRate> findByActiveTrueAndCoverageInOrderByDisplayOrderAsc(List<ShippingRate.Coverage> coverages);
}
