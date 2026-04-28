package com.example.medical_compare.model;

import java.util.List;
import java.util.Map;

public class ZeroStockWithMatches {

	private Product product; // الصنف المصفر
	private Map<String, Double> warehouseMatches; // المخازن والخصومات المتاحة

	public ZeroStockWithMatches(Product product, Map<String, Double> warehouseMatches) {
		this.product = product;
		this.warehouseMatches = warehouseMatches;
	}

	public Product getProduct() {
		return product;
	}

	public Map<String, Double> getWarehouseMatches() {
		return warehouseMatches;
	}

	public boolean isFound() {
		return warehouseMatches != null && !warehouseMatches.isEmpty();
	}
}
