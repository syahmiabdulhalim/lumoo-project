package com.example.lumoo.domain.pdpp;
import com.example.lumoo.domain.pdpp.ErasureRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ErasureRequestRepository extends JpaRepository<ErasureRequest, Long> {
    List<ErasureRequest> findByStatus(ErasureRequest.Status status);
    List<ErasureRequest> findByEmailOrderByRequestedAtDesc(String email);
    boolean existsByEmailAndStatus(String email, ErasureRequest.Status status);
}
