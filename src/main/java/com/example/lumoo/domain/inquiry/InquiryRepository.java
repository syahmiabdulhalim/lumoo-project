package com.example.lumoo.domain.inquiry;

import com.example.lumoo.domain.inquiry.Inquiry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    // Membolehkan Admin melihat pertanyaan terbaru di atas
List<Inquiry> findAllByOrderByCreatedAtDesc();}