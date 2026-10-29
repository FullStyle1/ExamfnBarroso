package com.fullstyle.examfnbarroso.catalog;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_products")
public class Product {

	@Id
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(nullable = false, length = 80)
	private String category;

	@Column(name = "price_usd", nullable = false, precision = 12, scale = 2)
	private BigDecimal priceUsd;

	protected Product() {
	}

	public Product(Long id, String name, String description, String category, BigDecimal priceUsd) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.priceUsd = priceUsd;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getCategory() {
		return category;
	}

	public BigDecimal getPriceUsd() {
		return priceUsd;
	}
}
