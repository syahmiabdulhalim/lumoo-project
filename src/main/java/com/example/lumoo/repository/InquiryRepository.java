package com.example.lumoo.repository;

import com.example.lumoo.model.Inquiry;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    // Membolehkan Admin melihat pertanyaan terbaru di atas
List<Inquiry> findAllByOrderByCreatedAtDesc();}