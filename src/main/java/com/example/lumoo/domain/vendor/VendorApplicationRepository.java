package com.example.lumoo.domain.vendor;

import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.vendor.VendorApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorApplicationRepository extends JpaRepository<VendorApplication, Long> {
    List<VendorApplication> findByStatusOrderByAppliedAtDesc(String status);
    List<VendorApplication> findByUserOrderByAppliedAtDesc(User user);
    Optional<VendorApplication> findByUserAndStatus(User user, String status);
    boolean existsByUserAndStatus(User user, String status);
}