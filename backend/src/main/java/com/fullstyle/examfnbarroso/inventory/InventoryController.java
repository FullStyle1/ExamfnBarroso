package com.fullstyle.examfnbarroso.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/microservices/inventory")
public class InventoryController {

	private final InventoryRepository inventory;

	public InventoryController(InventoryRepository inventory) {
		this.inventory = inventory;
	}

	@GetMapping("/{id}")
	public InventoryResponse getInventory(@PathVariable Long id) {
		Inventory item = inventory.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));

		return new InventoryResponse(item.getProductId(), item.getAvailableStock(), item.getWarehouseLocation());
	}
}
