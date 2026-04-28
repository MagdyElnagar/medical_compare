package com.example.medical_compare.Service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.medical_compare.Repository.ProductRebakRepository;
import com.example.medical_compare.model.product_rebak;

@Service
public class ProductRebakService {

    @Autowired
    private ProductRebakRepository repo;

    public void save(product_rebak p) {
        repo.save(p);
    }

    public List<product_rebak> findAll() {
        return repo.findAll();
    }

    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    public void deleteAll() {
        repo.deleteAll();
    }
}
