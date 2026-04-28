package com.example.medical_compare.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.medical_compare.model.Product;

public interface compareRepository extends JpaRepository<Product, Long> {
	
	
	
}
