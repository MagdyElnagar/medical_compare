package com.example.medical_compare.Repository;

//src/main/java/com/example/medical_compare/Repository/ReferenceProductRepository.java

import com.example.medical_compare.model.ReferenceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenceProductRepository extends JpaRepository<ReferenceProduct, Long> {}