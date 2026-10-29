package com.fullstyle.examfnbarroso.composite;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductSummaryController {

	private final ProductSummaryService productSummaryService;

	public ProductSummaryController(ProductSummaryService productSummaryService) {
		this.productSummaryService = productSummaryService;
	}

	@GetMapping({ "/api/v1/product-summary/{id}", "/api/v1/products/{id}/summary" })
	public ProductSummaryResponse getProductSummary(@PathVariable Long id) {
		return productSummaryService.getProductSummary(id);
	}
}
