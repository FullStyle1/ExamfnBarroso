package com.fullstyle.examfnbarroso.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_inventory")
public class Inventory {

	@Id
	private Long productId;

	@Column(name = "available_stock", nullable = false)
	private int availableStock;

	@Column(name = "warehouse_location", nullable = false, length = 120)
	private String warehouseLocation;

	@Column(name = "estimated_delivery_days", nullable = false)
	private int estimatedDeliveryDays;

	protected Inventory() {
	}

	public Inventory(Long productId, int availableStock, String warehouseLocation, int estimatedDeliveryDays) {
		this.productId = productId;
		this.availableStock = availableStock;
		this.warehouseLocation = warehouseLocation;
		this.estimatedDeliveryDays = estimatedDeliveryDays;
	}

	public Long getProductId() {
		return productId;
	}

	public int getAvailableStock() {
		return availableStock;
	}

	public String getWarehouseLocation() {
		return warehouseLocation;
	}

	public int getEstimatedDeliveryDays() {
		return estimatedDeliveryDays;
	}
}
