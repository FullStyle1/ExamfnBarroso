package com.fullstyle.examfnbarroso.catalog;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CatalogDataInitializer implements CommandLineRunner {

	private final ProductRepository products;

	public CatalogDataInitializer(ProductRepository products) {
		this.products = products;
	}

	@Override
	public void run(String... args) {
		if (!products.existsById(1L)) {
			products.save(new Product(
					1L,
					"Audifonos inalambricos",
					"Audifonos Bluetooth con cancelacion de ruido.",
					new BigDecimal("79.99")));
		}
	}
}
