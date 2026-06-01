package com.example.lumoo.domain.inquiry;
import com.example.lumoo.domain.inquiry.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByOrderByCreatedAtDesc();
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @Modifying @Transactional
    @Query("DELETE FROM Inquiry i WHERE i.createdAt < :cutoff")
    int deleteOldInquiries(@Param("cutoff") LocalDateTime cutoff);
}
