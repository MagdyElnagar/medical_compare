package com.example.medical_compare.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class product_rebak {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String prodName;
	private String store;
	private double discount;
	private double prize;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public double getPrize() { return prize; }
	public void setPrize(double prize) { this.prize = prize; }

	public double getDiscount() { return discount; }
	public void setDiscount(double discount) { this.discount = discount; }

	public String getProdName() { return prodName; }
	public void setProdName(String prodName) { this.prodName = prodName; }

	public String getStore() { return store; }
	public void setStore(String store) { this.store = store; }
}
