// src/main/java/com/example/medical_compare/Service/ReferenceProductService.java
package com.example.medical_compare.Service;

import com.example.medical_compare.MedicineService;
import com.example.medical_compare.Repository.ReferenceProductRepository;
import com.example.medical_compare.model.ReferenceProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReferenceProductService {

    @Autowired
    private ReferenceProductRepository repo;

    @Autowired
    private MedicineService medicineService;

    public void saveAll(List<ReferenceProduct> list) { repo.saveAll(list); }
    public void deleteAll() { repo.deleteAll(); }
    public List<ReferenceProduct> findAll() { return repo.findAll(); }

    /** بيرجع أعلى سعر مطابق بـ fuzzy أو 0 لو مفيش */
    public double findPrice(String name) {
        String cleanInput = medicineService.cleanMedicineName(name);
        return repo.findAll().stream()
            .filter(r -> medicineService.similarity(
                medicineService.cleanMedicineName(r.getName()), cleanInput) >= 0.88)
            .mapToDouble(ReferenceProduct::getPrice)
            .max()
            .orElse(0.0);
    }
}