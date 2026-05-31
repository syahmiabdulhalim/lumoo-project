package com.example.lumoo.domain.about;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyValueRepository extends JpaRepository<CompanyValue, Long> {
    List<CompanyValue> findAllByOrderByDisplayOrderAscTitleAsc();
}
