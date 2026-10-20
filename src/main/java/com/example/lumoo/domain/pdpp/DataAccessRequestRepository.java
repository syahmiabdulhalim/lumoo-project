package com.example.lumoo.domain.pdpp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DataAccessRequestRepository extends JpaRepository<DataAccessRequest, Long> {
    List<DataAccessRequest> findByEmailOrderByRequestedAtDesc(String email);
}
