package com.example.lumoo.domain.about;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findAllByOrderByDisplayOrderAscNameAsc();
    List<TeamMember> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
