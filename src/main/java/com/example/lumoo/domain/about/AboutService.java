package com.example.lumoo.domain.about;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AboutService {
    private static final Logger log = LoggerFactory.getLogger(AboutService.class);


    @Autowired private AboutContentRepository contentRepo;
    @Autowired private TeamMemberRepository teamRepo;
    @Autowired private CompanyValueRepository valueRepo;

    public AboutContent getContent() {
        return contentRepo.findById(1L).orElseGet(AboutContent::new);
    }

    public void saveContent(AboutContent content) {
        content.setId(1L);
        contentRepo.save(content);
    }

    public List<TeamMember> getAllMembers() {
        return teamRepo.findAllByOrderByDisplayOrderAscNameAsc();
    }

    public List<TeamMember> getActiveMembers() {
        return teamRepo.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public Optional<TeamMember> findMemberById(Long id) {
        return teamRepo.findById(id);
    }

    public void saveMember(TeamMember member) {
        teamRepo.save(member);
    }

    public void deleteMember(Long id) {
        teamRepo.deleteById(id);
    }

    public List<CompanyValue> getAllValues() {
        return valueRepo.findAllByOrderByDisplayOrderAscTitleAsc();
    }

    public Optional<CompanyValue> findValueById(Long id) {
        return valueRepo.findById(id);
    }

    public void saveValue(CompanyValue value) {
        valueRepo.save(value);
    }

    public void deleteValue(Long id) {
        valueRepo.deleteById(id);
    }
}
