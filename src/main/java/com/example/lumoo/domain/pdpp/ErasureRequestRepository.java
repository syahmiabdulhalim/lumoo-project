package com.example.lumoo.domain.pdpp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ErasureRequestRepository extends JpaRepository<ErasureRequest, Long> {
    List<ErasureRequest> findByEmailOrderByRequestedAtDesc(String email);
}
