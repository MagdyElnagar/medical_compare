// model/ZeroStockMatch.java

package com.example.medical_compare.model;

import java.util.Map;

import jakarta.persistence.*;

@Entity
public class ZeroStockWithMatches {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // للـ DB — بيتحفظ لما بنعمل fuzzy عند الرفع
    private Long productId;
    private String warehouseName;
    private double discount;

    // للعرض بس — مش بيتحفظ في DB
    @Transient
    private Product product;

    @Transient
    private Map<String, Double> warehouseMatches;

    // Constructor للعرض
    public ZeroStockWithMatches(Product product, Map<String, Double> warehouseMatches) {
        this.product = product;
        this.warehouseMatches = warehouseMatches;
    }

    // Constructor فاضي لـ JPA
    public ZeroStockWithMatches() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Map<String, Double> getWarehouseMatches() { return warehouseMatches; }
    public void setWarehouseMatches(Map<String, Double> warehouseMatches) { this.warehouseMatches = warehouseMatches; }
    public boolean isFound() { return warehouseMatches != null && !warehouseMatches.isEmpty(); }
}