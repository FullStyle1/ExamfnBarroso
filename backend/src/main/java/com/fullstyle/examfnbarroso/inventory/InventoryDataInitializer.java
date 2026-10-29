package com.fullstyle.examfnbarroso.inventory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InventoryDataInitializer implements CommandLineRunner {

	private final InventoryRepository inventory;

	public InventoryDataInitializer(InventoryRepository inventory) {
		this.inventory = inventory;
	}

	@Override
	public void run(String... args) {
		if (!inventory.existsById(1L)) {
			inventory.save(new Inventory(1L, 25, "Almacen central - Ciudad de Mexico"));
		}
	}
}
