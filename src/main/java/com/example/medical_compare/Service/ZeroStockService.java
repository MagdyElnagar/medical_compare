package com.example.medical_compare.Service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.medical_compare.Repository.ZeroStockRepository;
import com.example.medical_compare.model.Product;

@Service
public class ZeroStockService {

    @Autowired
    private ZeroStockRepository repo;

    public void save(Product p) {
        repo.save(p);
    }

    public List<Product> findAll() {
        return repo.findAll();
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}