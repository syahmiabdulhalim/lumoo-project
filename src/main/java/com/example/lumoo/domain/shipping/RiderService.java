package com.example.lumoo.domain.shipping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RiderService {

    @Autowired private RiderRepository repo;

    public List<Rider> getAll() {
        return repo.findAll(org.springframework.data.domain.Sort.by("name"));
    }

    public List<Rider> getActive() {
        return repo.findByActiveTrueOrderByNameAsc();
    }

    public Optional<Rider> findById(Long id) {
        return repo.findById(id);
    }

    public Rider save(Rider rider) {
        return repo.save(rider);
    }

    public void toggle(Long id) {
        repo.findById(id).ifPresent(r -> { r.setActive(!r.isActive()); repo.save(r); });
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void incrementDeliveries(Long id) {
        repo.findById(id).ifPresent(r -> {
            r.setTotalDeliveries(r.getTotalDeliveries() + 1);
            repo.save(r);
        });
    }
}
