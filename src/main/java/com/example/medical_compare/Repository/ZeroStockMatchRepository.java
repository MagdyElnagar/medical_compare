package com.example.medical_compare.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.example.medical_compare.model.ZeroStockWithMatches;



@EnableJpaRepositories
public interface ZeroStockMatchRepository extends JpaRepository<ZeroStockWithMatches, Long> {
	
	
    void deleteByProductId(Long productId);
    void deleteAll();
    List<ZeroStockWithMatches> findByProductId(Long productId);
}


