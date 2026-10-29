package com.fullstyle.examfnbarroso.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/microservices/products")
public class ProductController {

	private final ProductRepository products;

	public ProductController(ProductRepository products) {
		this.products = products;
	}

	@GetMapping("/{id}")
	public ProductResponse getProduct(@PathVariable Long id) {
		Product product = products.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getCategory(),
				product.getPriceUsd());
	}
}
